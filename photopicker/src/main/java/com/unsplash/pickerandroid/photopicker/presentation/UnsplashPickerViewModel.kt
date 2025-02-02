package com.unsplash.pickerandroid.photopicker.presentation

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.domain.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * View model for the picker screen.
 * This will use the repository to fetch the photos depending on the search criteria.
 * This is using rx binding.
 */
class UnsplashPickerViewModel constructor(private val repository: Repository) : BaseViewModel() {

    private val mPhotosLiveData = MutableLiveData<PagingData<UnsplashPhoto>>()
    val photosLiveData: LiveData<PagingData<UnsplashPhoto>> get() = mPhotosLiveData

    private val onQueryChangeFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            onQueryChangeFlow
                .debounce(500)
                .flatMapLatest { text ->
                    if (TextUtils.isEmpty(text)) {
                        repository.loadPhotos(UnsplashPhotoPicker.getPageSize())
                    } else {
                        repository.searchPhotos(text, UnsplashPhotoPicker.getPageSize())
                    }
                }
                .onEach {
                    mPhotosLiveData.postValue(it)
                }
                .collect()
        }
    }

    /**
     * To abide by the API guidelines,
     * you need to trigger a GET request to this endpoint every time your application performs a download of a photo
     *
     * @param photos the list of selected photos
     */
    fun trackDownloads(photos: List<UnsplashPhoto>) {
        repository.trackDownload(*photos.map { it.links.download_location }.toTypedArray())
    }

    fun onQueryChanged(query: String) {
        viewModelScope.launch {
            onQueryChangeFlow.emit(query)
        }
    }
}
