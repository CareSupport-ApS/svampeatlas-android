package com.noque.svampeatlas.view_models

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.maps.android.SphericalUtil
import com.hadilq.liveevent.LiveEvent
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getExifLocation
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RecognitionService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.util.Date

fun <T> initialObserveMutableLiveData(observer: Observer<T>): MutableLiveData<T> {
    val liveData = MutableLiveData<T>()
    liveData.observeForever(observer)
    return liveData
}

class NewObservationViewModel(val context: AddObservationFragment.Context, val id: Long, mushroomId: Int, imageFilePath: String?) : ViewModel() {
    companion object { private const val TAG = "NewObservationViewModel" }

    sealed class Prompt(@StringRes val title: Int, @StringRes val message: Int, vararg formatArgs: Any?, val action: Pair<Int, Int>) {
        class UseImageMetadata(val imageLocation: Location, val userLocation: Location?): Prompt(R.string.addObservationVC_useImageMetadata_title, R.string.addObservationVC_useImageMetadata_message, imageLocation.accuracy.toString(), action = Pair(R.string.addObservationVC_useImageMetadata_positive, R.string.addObservationVC_useImageMetadata_negative))
    }

    sealed class Notification(val type: MotionToastStyle, @StringRes val title: Int, @StringRes val message: Int,  val args: Array<out Any?>? = null) {
        class LocationInaccessible: Notification(MotionToastStyle.ERROR, R.string.newObservationError_noCoordinates_title, R.string.newObservationError_noCoordinates_message)
        class LocalityInaccessible : Notification(MotionToastStyle.ERROR, R.string.newObservationError_noLocality_title, R.string.newObservationError_noLocality_message)
        class ObservationUploaded(id: Int): Notification(MotionToastStyle.SUCCESS, R.string.addObservationVC_successfullUpload_title, R.string.observation_id, arrayOf(id))
        class ObservationUpdated : Notification(MotionToastStyle.SUCCESS, R.string.common_success, R.string.message_observationUpdated)
        class NoteSaved : Notification(MotionToastStyle.SUCCESS, R.string.message_noteSaved, R.string.message_noteSaved_message)
        class Deleted: Notification(MotionToastStyle.INFO, R.string.common_success, R.string.common_success)
        class NewObservationError(val error: UserObservation.Error): Notification(MotionToastStyle.ERROR, error.title, error.message)
        class Error(error: AppError2): Notification(MotionToastStyle.ERROR, error.title, error.message)
    }

    sealed class Event {
        object Reset: Event()
        class GoBack(val reload: Boolean): Event()
        class GoBackToRoot(val reload: Boolean): Event()
    }

    // If null during session - then we do not want to find predictions
    private var recognitionService: RecognitionService? = null
    private var isAwaitingCoordinatedBeforeSave = false

    val observationDate: LiveData<Date> get() = userObservation.observationDate
    val substrate: LiveData<Pair<Substrate, Boolean>?> get() = userObservation.substrate
    val vegetationType: LiveData<Pair<VegetationType, Boolean>?> get() = userObservation.vegetationType
    val hosts: LiveData<Pair<List<Host>, Boolean>?> get() = userObservation.hosts
    val locality: LiveData<Pair<Locality, Boolean>?> get() = userObservation.locality
    val notes: LiveData<String?> get() = userObservation.notes
    val ecologyNotes: LiveData<String?> get() = userObservation.ecologyNotes
    val images: LiveData<List<UserObservation.Image>> get() = userObservation.images
    val mushroom: LiveData<Pair<Mushroom, DeterminationConfidence>?> get() = userObservation.mushroom

    private val _user by lazy { MutableLiveData<User>() }
    private val _isLoading by lazy { MutableLiveData(false) }
    private val _coordinateState: MutableLiveData<State<Pair<Location, Boolean>>> by lazy { initialObserveMutableLiveData(Observer {
        // Whenever coordinate state is set, we have to update user observation too.
        userObservation.location.value = it.item
    }) }

    private val _localitiesState by lazy {MutableLiveData<State<List<Locality>>>() }
    private val _predictionResultsState by lazy { MutableLiveData<State<List<Prediction>>>(State.Empty()) }

    private val userObservation = ListenableUserObservation {
        event.postValue(Event.Reset)

            if (it.location != null) {
                it.location?.let { locationPair -> _coordinateState.value = State.Items(locationPair)}
            } else {
                _coordinateState.value = State.Empty()
            }

            if (it.locality != null) {
                _localitiesState.value = State.Items(listOfNotNull(it.locality?.first))
            } else {
                _localitiesState.value = State.Empty()
                it.location?.first?.let { getLocalities(it) }
            }
            recognitionService?.reset()
            _predictionResultsState.value = State.Empty()

        viewModelScope.launch {
            if (it.images.isNotEmpty() && context == AddObservationFragment.Context.UploadNote) {
                it.images.forEach { when (it) {
                    is UserObservation.Image.LocallyStored -> {recognitionService?.addPhotoToRequest(it.file) }
                    is UserObservation.Image.New ->  {}
                    else -> {}
                } }
            }
        }
        }

    val isLoading: LiveData<Boolean> = _isLoading
    val coordinateState: LiveData<State<Pair<Location, Boolean>>> get() = _coordinateState
    val localitiesState: LiveData<State<List<Locality>>> get() = _localitiesState
    val predictionResultsState: LiveData<State<List<Prediction>>> get() = _predictionResultsState
    val user: LiveData<User> get() = _user

    val notification by lazy { LiveEvent<Notification>() }
    val prompt by lazy { LiveEvent<Prompt>() }
    val event by lazy { LiveEvent<Event>() }


    init {
        viewModelScope.launch {
            RoomService.users.getUser().onSuccess {
                _user.value = it
            }
        }

        when (context) {
            AddObservationFragment.Context.New -> {
                recognitionService = RecognitionService()
                userObservation.set(UserObservation())
            }
            AddObservationFragment.Context.Note -> {
                userObservation.set(UserObservation())
            }
            AddObservationFragment.Context.FromRecognition -> {
                userObservation.set(UserObservation())
                if (mushroomId != 0) {
                    setMushroom(mushroomId)
                }
                imageFilePath?.let { appendImage(File(imageFilePath)) }
            }
            AddObservationFragment.Context.Edit -> {
               editObservation(id)
            }
            AddObservationFragment.Context.UploadNote -> {
                recognitionService = RecognitionService()
                editNote(id)
            }
            AddObservationFragment.Context.EditNote -> {
                editNote(id)
            }
        }
    }

    private fun editObservation(id: Long) {
        _isLoading.value = true
        if (id != 0L) {
            viewModelScope.launch {
                DataService.observationsRepository.getObservation(id.toInt()).apply {
                    onSuccess { observation ->
                        userObservation.set(UserObservation(observation))
                        _isLoading.postValue(false)
                    }
                    onError {
                        notification.postValue(Notification.Error(it))
                        _isLoading.postValue(false)
                    }
                }
                }
            }
        }

    private fun editNote(id: Long) {
        _isLoading.value = true
        viewModelScope.launch {
            RoomService.notesDao.getById(id).apply {
                onSuccess {
                    userObservation.set(UserObservation(newObservation = it))
                    _isLoading.postValue(false)
                }
            }
        }
    }

    fun setMushroom(taxonID: Int?) {
        if (taxonID != null && taxonID != 0) {
            val predictionResults = predictionResultsState.value?.item
            val predictionResult = predictionResults?.find { it.mushroom.id == taxonID }
            if (predictionResult != null && !predictionResults.isNullOrEmpty()) {
                setDeterminationNotes(Prediction.getNotes(predictionResult, predictionResults))
            } else if (!predictionResults.isNullOrEmpty()) {
                setDeterminationNotes(null)
            }

            viewModelScope.launch(Dispatchers.IO) {
                DataService.mushroomsRepository.getMushroom(taxonID).apply {
                    onSuccess {
                        userObservation.mushroom.postValue(Pair(it, DeterminationConfidence.CONFIDENT))
                    }
                    onError {}
                }
            }
        } else {
            userObservation.mushroom.value = null
            setDeterminationNotes(null)
        }
    }

    fun setDeterminationNotes(notes: String?) {
        userObservation.determinationNotes = notes
    }

    fun setConfidence(confidence: DeterminationConfidence) {
        userObservation.mushroom.value?.let {
            userObservation.mushroom.value = Pair(it.first, confidence)
        }
    }

    fun setObservationDate(date: Date) {
        userObservation.observationDate.value = date
    }

    fun setLocality(locality: Locality) {
        userObservation.locality.value = Pair(locality, false)
    }

    fun setLocalityLock(isLocked: Boolean) {
        userObservation.locality.value?.first?.let {  userObservation.locality.value = Pair(it, isLocked) }
    }

    fun setLocationLock(isLocked:Boolean) {
        userObservation.location.value?.let { _coordinateState.value = State.Items(Pair(it.first, isLocked)) }
    }

    // This functions is called by the location manager, when the state changes
    fun setCoordinateState(state: State<Location>) {
        fun setLocation(location: Location) {
            _coordinateState.value = State.Items(Pair(location, false))
            if (isAwaitingCoordinatedBeforeSave) {
               saveAsNote()
            } else {
                getLocalities(location)
            }
        }

        when (state) {
            is State.Error -> {
                if (isAwaitingCoordinatedBeforeSave) {
                    isAwaitingCoordinatedBeforeSave = false
                    _isLoading.postValue(false)
                }
                _coordinateState.postValue(State.Empty())
                notification.postValue(Notification.LocationInaccessible())
            }
            is State.Loading -> _coordinateState.postValue(State.Loading())
            is State.Items -> {
                when (val image = userObservation.images.value?.firstOrNull()) {
                    is UserObservation.Image.New -> {
                        val imageLocation = image.file.getExifLocation()
                        if (imageLocation != null && SphericalUtil.computeDistanceBetween(imageLocation.latLng, state.items.latLng) > imageLocation.accuracy) {
                            prompt.postValue(Prompt.UseImageMetadata(imageLocation, state.items))
                        } else {
                            setLocation(state.items)
                            if (state.item != null) userObservation.observationDate.value = state.item.date
                        }
                    }
                    else -> setLocation(state.items)
                }
            }
            else -> {}
        }
    }

    fun setSubstrate(substrate: Substrate, isLocked: Boolean) {
        userObservation.substrate.value = Pair(substrate, isLocked)
        SharedPreferences.saveSubstrateID(if (isLocked) substrate.id else null)
        if (isLocked) {
            viewModelScope.launch { RoomService.substrates.saveSubstrate(substrate) }
        }
    }

    fun setVegetationType(vegetationType: VegetationType, isLocked: Boolean) {
        userObservation.vegetationType.value = Pair(vegetationType, isLocked)
        SharedPreferences.saveVegetationTypeID(if (isLocked) vegetationType.id else null)
        if (isLocked) {
            viewModelScope.launch {
                RoomService.vegetationTypes.saveVegetationType(vegetationType)
            }
        }
    }

    fun appendHost(host: Host, isLocked: Boolean) {
        val value = userObservation.hosts.value?.first ?: listOf()
        userObservation.hosts.value = Pair(value + listOf(host), isLocked)

        if (isLocked) {
            SharedPreferences.saveHostsID(userObservation.hosts.value?.first?.map { it.id })
            viewModelScope.launch {
                RoomService.hosts.saveHosts(listOf(host))
            }
        } else {
            SharedPreferences.saveHostsID(null)
        }
    }

    fun setHostsLockedState(value: Boolean) {
        if (userObservation.hosts.value?.second != value)
            userObservation.hosts.value = Pair(userObservation.hosts.value?.first ?: mutableListOf(), value)
    }

    fun removeHost(host: Host, isLocked: Boolean) {
        val value = userObservation.hosts.value?.first
        userObservation.hosts.value = Pair(value?.filter { it.id != host.id } ?: listOf(), isLocked)

        if (isLocked) {
            SharedPreferences.saveHostsID(userObservation.hosts.value?.first?.map { it.id })
        } else {
            SharedPreferences.saveHostsID(null)
        }
    }

    fun appendImage(imageFile: File) {
        viewModelScope.launch {
            recognitionService?.addPhotoToRequest(imageFile)
        }

        userObservation.images.value = ((userObservation.images.value ?: listOf()) + listOf(UserObservation.Image.New(imageFile)))
        imageFile.getExifLocation()?.let { imageLocation ->
            _coordinateState.value?.item?.first?.let { coordinateLocation ->
                if (SphericalUtil.computeDistanceBetween(imageLocation.latLng, coordinateLocation.latLng) > imageLocation.accuracy) {
                   prompt.postValue(Prompt.UseImageMetadata(imageLocation, null))
                }
            }
        }
    }

    fun removeImageAt(position: Int) {
        fun handleChange() {
//            removedImage.postValue(position)
            if (userObservation.images.value?.count() == 0) {
                _predictionResultsState.value = State.Empty()
            }
            recognitionService?.reset()
            viewModelScope.launch {
                images.value?.forEach {
                    when (it) {
                        is UserObservation.Image.Hosted -> {}
                        is UserObservation.Image.LocallyStored -> recognitionService?.addPhotoToRequest(it.file)
                        is UserObservation.Image.New -> recognitionService?.addPhotoToRequest(it.file)
                    }
                }
            }
        }

        userObservation.images.value?.getOrNull(position).let { image ->
            when (image) {
                is UserObservation.Image.New -> {
                    image.file.delete()
                    userObservation.images.value = userObservation.images.value?.toMutableList()?.minusElement(image)
                    handleChange()
                }
                is UserObservation.Image.LocallyStored -> {
                    image.file.delete()
                    userObservation.images.value = userObservation.images.value?.toMutableList()?.minusElement(image)
                    handleChange()
                }
                is UserObservation.Image.Hosted -> {
                    Session.deleteImage(image.id) {
                        it.onError {
                            userObservation.images.postValue(userObservation.images.value)
                         /*   showNotification.postValue((Notification.ImageDeletionError(getApplication<MyApplication>().resources, it)))*/
                        }
                        it.onSuccess {
                            userObservation.images.postValue(userObservation.images.value?.toMutableList()?.minusElement(image))
                            handleChange()
                        }
                    }
                }
                else -> {}
            }
        }
    }

    fun promptPositive() {
        when (val prompt = prompt.value) {
            is Prompt.UseImageMetadata -> {
                userObservation.observationDate.value = prompt.imageLocation.date
                _coordinateState.value = State.Items(Pair(prompt.imageLocation, false))
                getLocalities(prompt.imageLocation)
            }
            else -> {}
        }
    }

    fun promptNegative() {
        when (val prompt = prompt.value) {
            is Prompt.UseImageMetadata -> {
                prompt.userLocation?.let {
                    _coordinateState.value = State.Items(Pair(prompt.userLocation, false))
                }
            }
            else -> {}
        }
    }

    fun setNotes(notes: String?) {
        userObservation.notes.value = notes
    }

    fun setEcologyNotes(ecologyNotes: String?) {
        userObservation.ecologyNotes.value = ecologyNotes
    }

    fun resetLocation() {
        userObservation.locality.value = null
        userObservation.location.value = null
        _localitiesState.value = State.Empty()
        _coordinateState.value = State.Empty()
    }

    private fun getLocalities(location: Location) {
        // If we are not in right context, we do not want to find locality. But we want to clear saved locality.
        if (context == AddObservationFragment.Context.Note || context == AddObservationFragment.Context.EditNote) {
            _localitiesState.value = State.Empty()
            userObservation.locality.value = null
            return
        }
        _localitiesState.value = State.Loading()
        viewModelScope.launch {
            DataService
                .getLocalities(TAG, location.latLng) { result ->
                    result.onSuccess {
                        _localitiesState.postValue(State.Items(it))
                        val locality = it.minBy {
                            SphericalUtil.computeDistanceBetween(
                                location.latLng,
                                it.location
                            ).toInt()
                        }

                        userObservation.locality.postValue(Pair(locality, false))
                    }
                    result.onError {
                        _localitiesState.value = State.Error(it)
                        if (context != AddObservationFragment.Context.Note)
                        notification.postValue(Notification.LocalityInaccessible())
                    }
                }
        }
    }

    var getPredictionsJob: Job? = null

    fun getPredictions() {
        getPredictionsJob?.cancel(null)
        val recognitionService = recognitionService ?: return
        getPredictionsJob = viewModelScope.launch {
            val substrate = substrate.value?.first
            val vegetationType = vegetationType.value?.first
            try {
                if (substrate != null && vegetationType != null) {
                    recognitionService.addMetadataToRequest(vegetationType, substrate, observationDate.value ?: Date())
                }

                _predictionResultsState.postValue(State.Loading())
                when (val result = recognitionService.getResults()) {
                    is Result.Error -> _predictionResultsState.postValue(
                        State.Error(
                            result.error.toAppError(
                                MyApplication.resources
                            )
                        )
                    )
                    is Result.Success -> {
                        val predictions =
                            DataService.mushroomsRepository.fetchMushrooms(
                                result.value
                            )
                        _predictionResultsState.postValue(
                            State.Items(
                                    predictions
                            )
                        )
                    }
                }
            } catch (error: Exception) {
                _predictionResultsState.postValue(State.Empty())
            }
        }
    }

    fun uploadNew(): Boolean {
        val error = userObservation.userObservation.isValid()
        if (error != null) {
            notification.postValue(
                Notification.NewObservationError(
                    error
                )
            )
            return false
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                Session.uploadObservation(userObservation.userObservation).apply {
                    onError { notification.postValue(Notification.Error(it)) }
                    onSuccess {
                        when (context) {
                            AddObservationFragment.Context.New,
                            AddObservationFragment.Context.FromRecognition -> {
                                notification.postValue(Notification.ObservationUploaded(it.first))
                                event.postValue(Event.Reset)
                            }
                            AddObservationFragment.Context.Edit,
                            AddObservationFragment.Context.Note,
                            AddObservationFragment.Context.EditNote,
                            AddObservationFragment.Context.UploadNote -> {
                                notification.postValue(Notification.ObservationUploaded(it.first))
                                RoomService.notesDao.delete(NewObservation(Date(id), Date(), null, null, null, null, null, null, null, null, null, listOf(), listOf()))
                                event.postValue(Event.GoBack(true))
                            }
                        }

                    }
                    _isLoading.postValue(false)
                }
            }
            return true
        }
                    }

    fun uploadChanges() {
        _isLoading.value = true
        viewModelScope.launch {
            Session.editObservation(id.toInt(), userObservation.userObservation).apply {
                onError {
                    notification.postValue(Notification.Error(it)); _isLoading.postValue(false) }
                onSuccess {
                    notification.postValue(Notification.ObservationUpdated())
                    event.postValue(Event.GoBack(true))
                    _isLoading.postValue(false)
                }
            }
        }
    }


    fun saveAsNote() {
        viewModelScope.launch {
            RoomService.notesDao.save(userObservation.userObservation.asNewObservation()).apply {
                onError { notification.postValue(Notification.Error(it)) }
                onSuccess {
                    notification.postValue(Notification.NoteSaved())
                    when (context) {
                        AddObservationFragment.Context.New,
                        AddObservationFragment.Context.FromRecognition,
                        AddObservationFragment.Context.Note -> {
                            userObservation.set(UserObservation())
                        }
                        AddObservationFragment.Context.UploadNote,
                        AddObservationFragment.Context.EditNote -> {
                            event.postValue(Event.GoBack(true))
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun onCleared() {
        userObservation.userObservation.deleteTempimages()
        DataService.clearRequestsWithTag(TAG)
        super.onCleared()
    }

    fun delete() {
        viewModelScope.launch {
        when (context) {
            AddObservationFragment.Context.New, AddObservationFragment.Context.FromRecognition -> userObservation.set(UserObservation())
            AddObservationFragment.Context.Edit -> {
                Session.deleteObservation(id.toInt()).apply {
                    onError { notification.postValue(Notification.Error(it)) }
                    onSuccess {
                        notification.postValue(Notification.Deleted())
                        event.postValue(Event.GoBackToRoot(true))
                    }
                }
            }
            AddObservationFragment.Context.Note -> userObservation.set(UserObservation())
            AddObservationFragment.Context.EditNote, AddObservationFragment.Context.UploadNote -> {
                RoomService.notesDao.delete(NewObservation(Date(id), Date(), null, null, null, null, null, null, null, null, null, listOf(), listOf())).apply {
                        onError { notification.postValue(Notification.Error(it)) }
                        onSuccess {
                            notification.postValue(Notification.Deleted())
                            userObservation.userObservation.deleteAllImages()
                            event.postValue(Event.GoBack(true))
                        }

                    }
            }
        }
        }
    }
}