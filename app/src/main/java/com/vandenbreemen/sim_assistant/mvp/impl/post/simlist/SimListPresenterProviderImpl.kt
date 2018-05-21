package com.vandenbreemen.sim_assistant.mvp.impl.post.simlist

import com.vandenbreemen.sim_assistant.api.presenter.SimListPresenterProvider
import com.vandenbreemen.sim_assistant.app.SimAssistantApp
import com.vandenbreemen.sim_assistant.mvp.google.groups.GoogleGroupsInteractor
import com.vandenbreemen.sim_assistant.mvp.impl.post.google.GooglePostCacheInteractor
import com.vandenbreemen.sim_assistant.mvp.impl.post.google.GooglePostContentLoader
import com.vandenbreemen.sim_assistant.mvp.impl.post.google.GooglePostRepository
import com.vandenbreemen.sim_assistant.mvp.mainscreen.SimSource
import com.vandenbreemen.sim_assistant.mvp.mainscreen.UserSettingsInteractor
import com.vandenbreemen.sim_assistant.mvp.post.PostRepository
import com.vandenbreemen.sim_assistant.mvp.post.simlist.SimListPresenter
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe

/**
 * <h2>Intro</h2>
 *
 * <h2>Other Details</h2>
 * @author kevin
 */
class SimListPresenterProviderImpl(
        val application:SimAssistantApp,
        private val userSettingsInteractor: UserSettingsInteractor,
                        private val googleGroupsInteractor: GoogleGroupsInteractor,
                        private val googleGroupCacheInteractor: GooglePostCacheInteractor
                                   ):SimListPresenterProvider {
    override fun getSimListPresenter(): Single<SimListPresenter> {
        return userSettingsInteractor.getUserSettings().flatMap<SimListPresenter> { userSettings->

            Single.create(SingleOnSubscribe<SimListPresenter> {
                var repository:PostRepository? = null

                if(SimSource.GOOGLE_GROUP.getId() == userSettings.dataSource){
                    val groups = googleGroupsInteractor.getGoogleGroups().blockingGet()
                    repository = GooglePostRepository(groups[0].groupName, GooglePostContentLoader(), googleGroupCacheInteractor)
                }

                it.onSuccess(SimListPresenterImpl(SimListModelImpl(repository!!)))
            })


        }
    }


}