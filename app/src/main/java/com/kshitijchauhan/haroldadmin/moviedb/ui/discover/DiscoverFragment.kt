package com.kshitijchauhan.haroldadmin.moviedb.ui.discover


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.kshitijchauhan.haroldadmin.moviedb.R
import com.kshitijchauhan.haroldadmin.moviedb.ui.BaseFragment
import com.kshitijchauhan.haroldadmin.moviedb.ui.UIState
import com.kshitijchauhan.haroldadmin.moviedb.ui.common.model.LoadingTask
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.MainViewModel
import com.mikepenz.itemanimators.AlphaInAnimator
import kotlinx.android.synthetic.main.fragment_discover.*

class DiscoverFragment : BaseFragment() {

    private val TASK_LOAD_DISCOVER_MOVIES = "load-discover-movies"

    private lateinit var mainViewModel: MainViewModel
    private lateinit var discoverViewModel: DiscoverViewModel
    private var moviesAdapter: MoviesAdapter? = null

    override val associatedUIState: UIState = UIState.DiscoverScreenState

    override fun notifyBottomNavManager() {
        mainViewModel.bottomNavManager.setBottomNavActiveState(this.associatedUIState)
    }

    companion object {
        fun newInstance() = DiscoverFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_discover, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        discoverViewModel = ViewModelProviders.of(this).get(DiscoverViewModel::class.java)

        discoverViewModel.apply {
            mainViewModel.addLoadingTask(LoadingTask(TASK_LOAD_DISCOVER_MOVIES))
            getPopularMovies()

            moviesUpdate.observe(viewLifecycleOwner, Observer {
                moviesAdapter?.updateList(it)
                mainViewModel.completeLoadingTask(TASK_LOAD_DISCOVER_MOVIES)
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val linearLayoutManager = LinearLayoutManager(context)
        moviesAdapter = MoviesAdapter(Glide.with(this), mutableListOf()) { id ->
            mainViewModel.updateStateTo(UIState.DetailsScreenState(id))
        }
        rvMovies.apply {
            layoutManager = linearLayoutManager
            adapter = moviesAdapter
            itemAnimator = AlphaInAnimator()
        }
    }

    override fun onDestroyView() {
        moviesAdapter = null
        super.onDestroyView()
    }
}
