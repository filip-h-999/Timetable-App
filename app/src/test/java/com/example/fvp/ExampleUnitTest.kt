package com.fifo.fvp

import com.fifo.fvp.Table
import de.sematre.dsbmobile.DSBMobile
import org.junit.Assert.assertEquals
import org.junit.Test


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {

        val dsbMobile = DSBMobile("147322", "wggpupils")
        val vPlan = dsbMobile
            .timeTables
            .filter{tt -> tt.groupName == "Vplan Schüler morgen" || tt.groupName == "Vertretung Foyer" }
            .last()




        println(vPlan)

        val stundenPlan = Table.data(vPlan.detail)
        val sb = StringBuilder()
        sb.append("<table class='mon_list'>")
        sb.append("<tr>")
        sb.append("<th>Stunde</th>")
        sb.append("<th>Vertreter</th>")
        sb.append("<th>Lehrer</th>")
        sb.append("<th>Fach</th>")
        sb.append("<th>Raum</th>")
        sb.append("<th>Text</th>")
        sb.append("</tr>")
        stundenPlan.values.flatten().forEach{ row ->
            run {
                sb.append("<tr>")
                sb.append("<td>${row.stunde}</td>")
                sb.append("<td>${row.vertreter}</td>")
                sb.append("<td>${row.lehrer}</td>")
                sb.append("<td>${row.fach}</td>")
                sb.append("<td>${row.raum}</td>")
                sb.append("<td>${row.text}</td>")
                sb.append("</tr>")
            }
        }
        sb.append("</table>")
        println(String(sb.toString().toByteArray()))

        assertEquals(4, 2 + 2)
    }
}