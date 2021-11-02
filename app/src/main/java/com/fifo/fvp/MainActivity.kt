package com.fifo.fvp

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.View
import android.webkit.WebView
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.fifo.fvp.login.LoginActivity
import de.sematre.dsbmobile.DSBMobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.UnknownHostException


class MainActivity : AppCompatActivity() {


    private lateinit var web: WebView
    private lateinit var klassenSpinner: Spinner
    private lateinit var wifiOffline: ImageView
    private lateinit var template: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        val alreadyLoggedIn = getSharedPreferences("preferences", Context.MODE_PRIVATE)
            .getBoolean("already_logged_in", false)

        if (!alreadyLoggedIn) {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finishActivity(0)
        } else {

            setContentView(R.layout.activity_main)
            setSupportActionBar(findViewById(R.id.toolbar))

            klassenSpinner = findViewById(R.id.klassenSpinner)
            wifiOffline = findViewById(R.id.wifiOffline)
            wifiOffline.setOnClickListener { view ->
                val isConnected: Boolean = runBlocking(Dispatchers.IO) {

                        isInternetAvailable()
                }
                    if (isConnected) {
                        clearTable(web)
                        val selectedKlasse =
                            klassenSpinner.getItemAtPosition(readeClassIndexFromKDisk())
                                .toString()

                        Handler(Looper.getMainLooper()).postDelayed({
                            drawTableFromDataOnDisk(selectedKlasse)
                            wifiOffline.visibility = View.INVISIBLE
                        }, 200)

                    }


            }
            setWebView()

            template = resources.openRawResource(R.raw.template)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }


            runBlocking {
                launch(Dispatchers.IO) {
                    if (isInternetAvailable()) {
                        wifiOffline.visibility = View.INVISIBLE
                        val username =
                            getSharedPreferences("preferences", Context.MODE_PRIVATE).getString("usr", null)
                        val password =
                            getSharedPreferences("preferences", Context.MODE_PRIVATE).getString("pwd", null)
                        val dsbMobile = DSBMobile(username, password)
                        val vPlans = dsbMobile
                            .timeTables
                            .filter { tt -> tt.groupName == "Vplan Schüler morgen" || tt.groupName == "Vertretung Foyer" }

                        saveDataOnDisk(vPlans)
                    }else {
                        wifiOffline.visibility = View.VISIBLE
                    }
                }
            }

            findViewById<Spinner>(R.id.klassenSpinner).setSelection(readeClassIndexFromKDisk())

            klassenSpinner.onItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {

                        val selectedKlasse = parent?.getItemAtPosition(position).toString()
                        clearTable(web)
                        saveSelectedClassOnDisk(position)
                        drawTableFromDataOnDisk(selectedKlasse)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        TODO("Not yet implemented")
                    }
                }
        }


    }

    private fun setWebView() {
        web = findViewById<WebView>(R.id.webView)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.setBackgroundColor(0);
        web.setBackgroundResource(R.drawable.background)
    }


    private fun saveSelectedClassOnDisk(klasseIndex: Int) {
        getSharedPreferences("preferences", 0).edit().putInt("klasse-index", klasseIndex).apply()
    }

    private fun readeClassIndexFromKDisk(): Int {
        return getSharedPreferences("preferences", 0).getInt("klasse-index", 0)
    }

    private fun saveDataOnDisk(data: List<DSBMobile.TimeTable>) {
        writeToFile(
            "Heute", Jsoup.connect(data.first().detail)
                .execute()
                .charset("windows-1252")
                .body()
        )


        writeToFile(
            "Morgen", Jsoup.connect(data.last().detail)
                .execute()
                .charset("windows-1252")
                .body()
        )
    }

    private fun drawTableFromDataOnDisk(klasse: String) {
        val todayFile = File(filesDir, "Heute")
        val tomorrowFile = File(filesDir, "Morgen")

        val dateToday = Table.extractDate(todayFile)
        val planToday = Table.data(todayFile)

        val dateTomorrow = Table.extractDate(tomorrowFile)
        val planTomorrow = Table.data(tomorrowFile)

        clearTable(web)
        val html = template
            .replace("###TABLE_TODAY###", buildTableHTML(planToday[klasse]))
            .replace("###TABLE_TOMORROW###", buildTableHTML(planTomorrow[klasse]))
            .replace("###TODAY###", dateToday)
            .replace("###TOMORROW###", dateTomorrow)

        println(html)
        web.loadData(b64(html), "text/html; charset=UTF-8", "base64")
    }

    private fun isInternetAvailable(): Boolean {
        try {
            val address: InetAddress = InetAddress.getByName("www.google.com")
            return !address.equals("")
        } catch (e: UnknownHostException) {
            // Log error
        }
        return false
    }

    private fun writeToFile(fileNme: String, data: String): File {
        val file = File(filesDir, fileNme)
        val fileOutputStream = FileOutputStream(file)
        val outputStreamWriter = OutputStreamWriter(fileOutputStream)
        val bufferedWriter = BufferedWriter(outputStreamWriter)
        bufferedWriter.write(data)
        bufferedWriter.close()
        return file
    }

    private fun clearTable(web: WebView) {
        web.loadData("</htm>", null, null)
    }

    private fun b64(s: String): String {
        return android.util.Base64.encodeToString(
            s.toByteArray(charset("UTF-8")),
            android.util.Base64.DEFAULT
        )
    }

    private fun buildTableHTML(rows: List<Row>?): String {
        val sb = StringBuilder()
        sb.append("<table class='mon_list'>")
        //sb.append("<tr class='header'>dsdds</tr>")
        sb.append("<tr class='header'>")
        sb.append("<th>Stunde</th>")
        sb.append("<th>Vertreter</th>")
        sb.append("<th>Lehrer</th>")
        sb.append("<th>Fach</th>")
        sb.append("<th>Raum</th>")
        sb.append("<th>Text</th>")
        sb.append("</tr>")


        val str = """
            <tr class='list even'>
                <td colspan='6'>
                    &nbsp;</br>&nbsp;
                </td>
            </tr>
            
        """.trimIndent()

        if (rows == null || rows.isEmpty()) {
            sb.append(str)
            sb.append("</table>")
            return sb.toString()
        }


        rows.mapIndexed { i, row ->
            var clazz = if (i % 2 == 0) "list even" else "list odd"
            if (
                (row.vertreter.replace(" ", "") == "+")
                || (row.text.contains("fällt", true))
                || (row.text.contains("f.a.", true))
            )
                clazz += " skip"


            sb.append("<tr class='$clazz'>")
            sb.append("<td>${row.stunde}</td>")
            sb.append("<td>${row.vertreter}</td>")
            sb.append("<td>${row.lehrer}</td>")
            sb.append("<td>${row.fach}</td>")
            sb.append("<td>${row.raum}</td>")
            sb.append("<td>${row.text}</td>")
            sb.append("</tr>")
        }
        sb.append("</table>")
        return sb.toString()
    }


}