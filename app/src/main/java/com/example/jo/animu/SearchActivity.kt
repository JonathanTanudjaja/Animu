package com.example.jo.animu

import SearchQuery
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.jo.animu.adapter.AnimeListAdapter
import com.example.jo.animu.model.Anime
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import kotlinx.android.synthetic.main.activity_search.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class SearchActivity : AppCompatActivity() {

    private var apolloClient: ApolloClient? = null

    private val _baseUrl: String = "https://graphql.anilist.co"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(searchBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        searchRv.layoutManager = LinearLayoutManager(this,
                LinearLayout.VERTICAL, false)

        searchRv.adapter = AnimeListAdapter(MutableList(0,{_ ->
            Anime(null, null, null, null)}), this)

        apolloClient = setupApollo()

        Observable.create(ObservableOnSubscribe<String> { subscriber ->
            searchET.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    subscriber.onNext(s.toString())
                }

            })
        }).debounce(800, TimeUnit.MILLISECONDS).subscribe({ text ->
            onQuery(text)
        })

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (Intent.ACTION_SEARCH.equals(intent?.action)) {
            val query = intent?.getStringExtra(SearchManager.QUERY)

            Log.i("test", query)
        }
    }

    private fun setupApollo(): ApolloClient {
        val okHttp = OkHttpClient.Builder()
                .addInterceptor({ chain ->
                    val original = chain.request()
                    val builder = original.newBuilder()
                            .method(original.method(), original.body())

                    chain.proceed(builder.build())
                })
                .build()

        return ApolloClient.builder()
                .serverUrl(_baseUrl)
                .okHttpClient(okHttp)
                .build()
    }

    private fun onQuery(text: String) {

        runOnUiThread {
            loader.visibility = View.VISIBLE
            searchRv.visibility = View.INVISIBLE
        }

        apolloClient!!.query(SearchQuery
                .builder()
                .query(text)
                .page(1)
                .perPage(4)
                .build())
                .enqueue(object : ApolloCall.Callback<SearchQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        runOnUiThread {
                            Log.e(" ", e.printStackTrace().toString())
                            Toast.makeText(this@SearchActivity,
                                    "Failed to get anime list",
                                    Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }

                    override fun onResponse(response: Response<SearchQuery.Data>) {
                        Log.i("test", response.data()?.toString())
                        val animeList: MutableList<Anime> = response.data()?.Page()?.media()?.map {
                            Anime(it.id(),
                                    it.title()?.romaji(),
                                    it.averageScore(),
                                    it.coverImage()?.medium())
                        }?.toMutableList()!!

                        runOnUiThread {
                            loader.visibility = View.INVISIBLE
                            searchRv.visibility = View.VISIBLE
                            searchRv.adapter = AnimeListAdapter(animeList, this@SearchActivity)
                            searchRv.adapter.notifyDataSetChanged()
                        }
                    }
                })
    }
}
