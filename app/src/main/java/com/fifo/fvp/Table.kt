package com.fifo.fvp

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File

data class Row(
    val stunde: String,
    val vertreter: String,
    val lehrer: String,
    val fach: String,
    val raum: String,
    val text: String
)

object Table {

    fun data(file: File): Map<String, List<Row>> {
        val doc: Document = Jsoup.parse(file, "UTF-8")
        return data(doc)
    }

    fun data(url: String): Map<String, List<Row>> {
        val doc = Jsoup.connect(url).execute().charset("windows-1252").parse()

        return data(doc)
    }



    fun extractDate(file: File): String {
        val doc: Document = Jsoup.parse(file, "UTF-8")
        return doc
            .select("body > center:nth-child(2) > div")
            .first()!!
            .text()
    }

    private fun data(doc: Document): Map<String, List<Row>> {
        val map: HashMap<String, List<Row>> = HashMap()
        var klasse = "undefined"
        doc
            .select(".mon_list > tbody:nth-child(1)")
            .first()
            ?.children()
            ?.drop(1) //drop table headers
            ?.forEach { tr ->
                if (tr.children().size == 1) { //extract klasse e.g. 7A
                    klasse = tr.children().first()!!.text()
                    map[klasse] = ArrayList()
                } else {
                    val tds = tr.children()
                    val row = Row(
                        tds[0].text(),
                        tds[1].text(),
                        tds[2].text(),
                        tds[3].text(),
                        tds[4].text(),
                        tds[5].text()
                    )
                    map[klasse] = map.getValue(klasse).plus(row)
                }
            }
        return map
    }
}