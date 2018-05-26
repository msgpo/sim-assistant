package com.vandenbreemen.sim_assistant.fragments

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.vandenbreemen.sim_assistant.R
import com.vandenbreemen.sim_assistant.R.id.viewSims
import com.vandenbreemen.sim_assistant.ViewSimActivity
import com.vandenbreemen.sim_assistant.api.sim.Sim
import com.vandenbreemen.sim_assistant.mvp.post.simlist.SimListPresenter
import com.vandenbreemen.sim_assistant.mvp.post.simlist.SimListView
import java.text.SimpleDateFormat
import java.util.*

/**
 * <h2>Intro</h2>
 *
 * <h2>Other Details</h2>
 * @author kevin
 */
class SimListFragment: Fragment(), SimListView {

    private lateinit var presenter:SimListPresenter

    lateinit var currentList:MutableList<Sim>

    lateinit var adapter: ArrayAdapter<Sim>

    val simToUiComponent: MutableMap<Sim, CardView> = mutableMapOf()

    fun setPresenter(presenter: SimListPresenter){
        this.presenter = presenter
        this.currentList = mutableListOf<Sim>()
    }

    override fun viewSelectedSims(sims: List<Sim>) {
        val intent = Intent(activity, ViewSimActivity::class.java)

        val arrayOfSims = Array(sims.size, {index->sims[index]})
        intent.putExtra(ViewSimActivity.PARM_SIMS, arrayOfSims)
        activity.startActivity(intent)
    }

    override fun viewSim(sim: Sim) {
        val intent = Intent(activity, ViewSimActivity::class.java)
        intent.putExtra(ViewSimActivity.PARM_SIMS, arrayOf(sim))
        startActivity(intent)
    }

    override fun addSimItem(sim: Sim) {
        currentList.add(sim)
        adapter.notifyDataSetChanged()
    }

    override fun displayViewSelectedSimsOption() {
        view.findViewById<View>(viewSims).visibility = VISIBLE
    }

    override fun selectSim(sim: Sim) {
        simToUiComponent[sim]!!.setCardBackgroundColor(resources.getColor(R.color.selectedSim, context.theme))
    }

    fun deselectSim(sim: Sim) {

    }

    private fun createSimListView(inflater: LayoutInflater, layout:ViewGroup){
        val listView = layout.findViewById<ListView>(R.id.simList)
        this.adapter = object:ArrayAdapter<Sim>(
                context,
                android.R.layout.simple_list_item_1,
                currentList
        ){
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
                val sim = currentList[position]
                val cardView = inflater.inflate(R.layout.layout_sim_list_item, parent, false) as CardView
                cardView.findViewById<TextView>(R.id.simTitle).setText(sim.title)
                cardView.findViewById<TextView>(R.id.simAuthor).setText(sim.author)
                cardView.findViewById<TextView>(R.id.simDate).setText(simpleDateFormat.format(Date(sim.postedDate)))

                cardView.setOnClickListener(View.OnClickListener { view ->
                    presenter.viewSim(sim)
                })

                cardView.setOnLongClickListener(View.OnLongClickListener { view ->
                    presenter.selectSim(sim)
                    true
                })

                simToUiComponent.put(sim, cardView)

                return cardView
            }
        }
        listView.adapter = adapter
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.layout_sim_list, container, false) as ViewGroup
        createSimListView(inflater, layout)

        //  Set up the FAB
        layout.findViewById<View>(R.id.viewSims).setOnClickListener(View.OnClickListener { view ->
            presenter.viewSelectedSims()
        })

        presenter.start(this)

        return layout
    }

}