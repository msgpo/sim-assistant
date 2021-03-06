package com.vandenbreemen.sim_assistant.mvp.impl.post.simlist

import com.vandenbreemen.sim_assistant.api.sim.Sim
import com.vandenbreemen.sim_assistant.mvp.post.PostRepository
import com.vandenbreemen.sim_assistant.mvp.post.simlist.SimListModel
import io.reactivex.Observable
import java.util.*

/**
 * <h2>Intro</h2>
 *
 * <h2>Other Details</h2>
 * @author kevin
 */
class SimListModelImpl(val postRepository: PostRepository): SimListModel {

    companion object {
        val DEFAULT_NUM_POSTS = 100
    }

    val selectedSims = mutableListOf<Sim>()

    override fun getSimList(): Observable<Sim> {
        return postRepository.getPosts(DEFAULT_NUM_POSTS)
    }

    override fun selectedSims(): List<Sim> {
        val copyOfSelected = ArrayList<Sim>(selectedSims)
        return Collections.unmodifiableList(copyOfSelected.sortedBy { sim->sim.postedDate })
    }

    override fun hasSelectedSims(): Boolean {
        return selectedSims.isNotEmpty()
    }

    override fun simSelected(sim: Sim): Boolean {
        return selectedSims.contains(sim)
    }

    override fun deselectSim(sim: Sim) {
        this.selectedSims.remove(sim)
        sim.selected = false
    }

    override fun selectSim(sim: Sim) {
        selectedSims.add(sim)
        sim.selected = true
    }
}