package com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.people

import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.service.common.ErrorResponse
import com.kshitijchauhan.haroldadmin.moviedb.repository.data.remote.utils.NetworkResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface PersonService {

    @GET("person/{personId}")
    fun getPerson(@Path("personId") id: Int): Single<NetworkResponse<PersonResponse, ErrorResponse>>

}