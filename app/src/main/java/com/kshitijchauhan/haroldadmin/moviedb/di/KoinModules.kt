package com.kshitijchauhan.haroldadmin.moviedb.di

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.kshitijchauhan.haroldadmin.moviedb.BuildConfig
import com.kshitijchauhan.haroldadmin.moviedb.R
import com.kshitijchauhan.haroldadmin.moviedb.repository.actors.ActorsRepository
import com.kshitijchauhan.haroldadmin.moviedb.repository.actors.LocalActorsSource
import com.kshitijchauhan.haroldadmin.moviedb.repository.actors.RemoteActorsSource
import com.kshitijchauhan.haroldadmin.moviedb.repository.collections.CollectionsRepository
import com.kshitijchauhan.haroldadmin.moviedb.repository.collections.LocalCollectionsSource
import com.kshitijchauhan.haroldadmin.moviedb.repository.collections.RemoteCollectionsSource
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.local.db.MovieDBDatabase
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.ApiKeyInterceptor
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.ApiManager
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.utils.Config
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.SessionIdInterceptor
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.account.AccountService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.auth.AuthenticationService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.discover.DiscoveryService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.movie.MovieService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.people.PersonService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.search.SearchService
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.utils.KotlinRxJava2CallAdapterFactory
import com.kshitijchauhan.haroldadmin.moviedb.repository.movies.LocalMoviesSource
import com.kshitijchauhan.haroldadmin.moviedb.repository.movies.MoviesRepository
import com.kshitijchauhan.haroldadmin.moviedb.repository.movies.RemoteMoviesSource
import com.kshitijchauhan.haroldadmin.moviedb.ui.UIState
import com.kshitijchauhan.haroldadmin.moviedb.ui.actor_details.ActorDetailsEpoxyController
import com.kshitijchauhan.haroldadmin.moviedb.ui.actor_details.ActorDetailsViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.auth.AuthenticationViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.common.EpoxyCallbacks
import com.kshitijchauhan.haroldadmin.moviedb.ui.in_theatres.InTheatresEpoxyController
import com.kshitijchauhan.haroldadmin.moviedb.ui.in_theatres.InTheatresViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.library.LibraryEpoxyController
import com.kshitijchauhan.haroldadmin.moviedb.ui.library.LibraryViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.HomeEpoxyController
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.HomeViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.main.MainViewModel
import com.kshitijchauhan.haroldadmin.moviedb.ui.movie_details.DetailsEpoxyController
import com.kshitijchauhan.haroldadmin.moviedb.ui.movie_details.MovieDetailsViewModel
import com.kshitijchauhan.haroldadmin.moviedb.utils.Constants
import com.kshitijchauhan.haroldadmin.moviedb.utils.SafeRfc3339DateJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*

val applicationModule = module {
    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            androidContext().getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
    }
}

val retrofitModule = module {
    single { Cache(androidContext().cacheDir, 50 * 1024 * 1024) }

    single { (sessionId: String) -> SessionIdInterceptor(sessionId) }

    single { (apiKey: String) -> ApiKeyInterceptor(apiKey) }

    single { (loggingLevel: HttpLoggingInterceptor.Level) ->
        HttpLoggingInterceptor().apply {
            level = loggingLevel
        }
    }

    single {

        val apiKey = BuildConfig.API_KEY
        val sessionId = get<SharedPreferences>().getString(Constants.KEY_SESSION_ID, "")

        OkHttpClient.Builder()
            .addInterceptor(get<ApiKeyInterceptor> {
                parametersOf(apiKey)
            })
            .addInterceptor(get<SessionIdInterceptor> {
                parametersOf(sessionId)
            })
            .also {
                if (BuildConfig.DEBUG)
                    it.addInterceptor(get<HttpLoggingInterceptor> {
                        parametersOf(HttpLoggingInterceptor.Level.BASIC)
                    })
            }
            .cache(get())
            .build()
    }

    single {
        val adapter = SafeRfc3339DateJsonAdapter(Rfc3339DateJsonAdapter()).nullSafe()
        Moshi.Builder()
            .add(Date::class.java, adapter)
            .build()
            .let { moshi -> MoshiConverterFactory.create(moshi) }
    }

    single { RxJava2CallAdapterFactory.create() }

    single { KotlinRxJava2CallAdapterFactory.create() }

    single {
        Retrofit.Builder()
            .client(get())
            .addCallAdapterFactory(get<KotlinRxJava2CallAdapterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .addConverterFactory(get<MoshiConverterFactory>())
            .baseUrl(Config.BASE_URL)
            .build()
    }
}

val databaseModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            MovieDBDatabase::class.java,
            androidContext().getString(R.string.app_name)
        ).build()
    }
}

val apiModule = module {
    single { get<Retrofit>().create(AuthenticationService::class.java) }
    single { get<Retrofit>().create(DiscoveryService::class.java) }
    single { get<Retrofit>().create(SearchService::class.java) }
    single { get<Retrofit>().create(MovieService::class.java) }
    single { get<Retrofit>().create(AccountService::class.java) }
    single { get<Retrofit>().create(PersonService::class.java) }
    single { ApiManager(get(), get(), get(), get(), get()) }
}

val repositoryModule = module {
    single { get<MovieDBDatabase>().moviesDao() }
    single { get<MovieDBDatabase>().actorsDao() }
    single { get<MovieDBDatabase>().collectionsDao() }

    single { LocalMoviesSource(get(), get()) }
    single { RemoteMoviesSource(get(), get(), get()) }
    single { LocalActorsSource(get()) }
    single { RemoteActorsSource(get()) }
    single { LocalCollectionsSource(get(), get()) }
    single { RemoteCollectionsSource(get(), get()) }

    single { MoviesRepository(get(), get()) }
    single { ActorsRepository(get(), get()) }
    single { CollectionsRepository(get(), get(), get()) }
}

val uiModule = module {

    viewModel { (initialState: UIState.HomeScreenState) ->
        HomeViewModel(get(), get(), initialState)
    }

    viewModel { (accountId: Int, initialState: UIState.LibraryScreenState) ->
        LibraryViewModel(get(), accountId, initialState)
    }

    viewModel { (initialState: UIState.InTheatresScreenState) ->
        InTheatresViewModel(get(), initialState)
    }

    viewModel { AuthenticationViewModel(get(), get(), get()) }
    viewModel { (isAuthenticated: Boolean, movieId: Int, initialState: UIState.DetailsScreenState) ->
        MovieDetailsViewModel(isAuthenticated, movieId, get(), initialState)
    }
    viewModel { MainViewModel(get()) }

    viewModel { (actorId: Int, initialState: UIState.ActorDetailsScreenState) ->
        ActorDetailsViewModel(actorId, get(), initialState)
    }

    factory("fragment-glide-request-manager") { (fragment: Fragment) ->
        Glide.with(fragment)
    }

    factory("view-glide-request-manager") { (view: View) ->
        Glide.with(view)
    }

    factory { (callbacks: DetailsEpoxyController.MovieDetailsCallbacks, glide: RequestManager) ->
        DetailsEpoxyController(callbacks, glide, get<Handler>("epoxy-handler"))
    }

    factory { (callbacks: EpoxyCallbacks, glide: RequestManager) ->
        HomeEpoxyController(callbacks, glide, get("epoxy-handler"))
    }

    factory { (callbacks: EpoxyCallbacks, glide: RequestManager) ->
        LibraryEpoxyController(callbacks, glide, get("epoxy-handler"))
    }

    factory { (callbacks: EpoxyCallbacks, glide: RequestManager) ->
        InTheatresEpoxyController(callbacks, glide, get("epoxy-handler"))
    }

    factory { ActorDetailsEpoxyController(get("epoxy-handler")) }

    single("epoxy-handler-thread") {
        HandlerThread("epoxy").apply {
            start()
        }
    }

    single("epoxy-handler") {
        val handlerThread = get<HandlerThread>("epoxy-handler-thread")
        Handler(handlerThread.looper)
    }
}


