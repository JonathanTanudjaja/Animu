package com.example.jo.animu

import BrowseQuery
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.example.jo.animu.adapter.AnimeListAdapter
import com.example.jo.animu.model.Anime
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    private var apolloClient: ApolloClient? = null

    private val _baseUrl: String = "https://graphql.anilist.co"

    private var currIndex : Long = 1
    private var index : Long = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animeRv.layoutManager = LinearLayoutManager(this,
                LinearLayout.VERTICAL, false)

        animeRv.adapter = AnimeListAdapter(MutableList(0, { _ ->
            Anime(null, null, null, null) }), this)

        apolloClient = setupApollo()

        query()

        refreshLayout.setOnRefreshListener {
            index = 1
            currIndex = 1
            refreshLayout.isRefreshing = true
            query()
        }

        attachScrollListener()
    }

    fun attachScrollListener() {
        animeRv.clearOnScrollListeners()
        animeRv.addOnScrollListener(InfiniteScrollListener({ index++;query() }, animeRv.layoutManager as LinearLayoutManager))
    }

    class InfiniteScrollListener(val func: () -> Unit, val layoutManager: LinearLayoutManager) : RecyclerView.OnScrollListener() {

        private var previousTotal = 0
        private var loading = true
        private var visibleThreshold = 3
        private var firstVisibleItem = 0
        private var visibleItemCount = 0
        private var totalItemCount = 0

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (dy > 0) {
                visibleItemCount = recyclerView.childCount
                totalItemCount = layoutManager.itemCount
                firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false
                        previousTotal = totalItemCount
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount)
                        <= (firstVisibleItem + visibleThreshold)) {
                    // End has been reached
                    Log.i("InfiniteScrollListener", "End reached")
                    func()
                    loading = true
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        if (item?.itemId?.equals(R.id.search)!!) {
            startActivity(Intent(this, SearchActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun query() {
        apolloClient!!.query(BrowseQuery
                .builder()
                .page(index)
                .perPage(10)
                .build())
                .enqueue(object : ApolloCall.Callback<BrowseQuery.Data>() {
                    override fun onFailure(e: ApolloException) {
                        runOnUiThread {
                            Log.e(" ", e.printStackTrace().toString())
                            Toast.makeText(this@MainActivity,
                                    "Failed to get anime list",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            if (refreshLayout.isRefreshing) {
                                refreshLayout.isRefreshing = false
                            }
                        }
                    }

                    override fun onResponse(response: Response<BrowseQuery.Data>) {
                        Log.i("test", response.data()?.toString())
                        val animeList: MutableList<Anime> = response.data()?.Page()?.mediaTrends()?.map {
                            Anime(it.media()?.id(),
                                    it.media()?.title()?.romaji(),
                                    it.media()?.averageScore(),
                                    it.media()?.coverImage()?.medium())
                        }?.toMutableList()!!

                        runOnUiThread {
                            if (index == currIndex) {
                                (animeRv.adapter as AnimeListAdapter).changeDataSet(animeList)
                                attachScrollListener()
                            } else {
                                currIndex = index
                                (animeRv.adapter as AnimeListAdapter).add(animeList)
                            }

                            if (refreshLayout.isRefreshing) {
                                refreshLayout.isRefreshing = false
                            }

                        }
                    }
                })
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
}
