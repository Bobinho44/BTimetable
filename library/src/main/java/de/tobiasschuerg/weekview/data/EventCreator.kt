package de.tobiasschuerg.weekview.data

import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import de.tobiasschuerg.weekview.util.TimeSpan
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.threeten.bp.Clock
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import java.util.Random

object EventCreator {

    private const val url = "https://edt.univ-nantes.fr/sciences/g351176.xml"
    private val doc: Document = Jsoup.connect(url).parser(Parser.xmlParser()).get()

    private val random = Random()

    @RequiresApi(Build.VERSION_CODES.O)
    fun daysData(days: List<LocalDate>): List<Event.Single> {

        return days
            .flatMap { date ->
                val firstWeekDay = date.minusDays(date.dayOfWeek.value - 1L)
                val week = doc.select("span")
                    .filter { it ->
                        val dateInformation = it.attr("date").split("/")
                        LocalDate.of(
                            dateInformation[2].toInt(),
                            dateInformation[1].toInt(),
                            dateInformation[0].toInt()
                        ).equals(firstWeekDay)
                    }
                    .map { it.select("alleventweeks").text()}
                    .first()

                doc.select("event")
                    .filter { it -> it.select("rawweeks").text() == week }
                    .filter { it.select("day").text().toInt() + 1 == date.dayOfWeek.value }
                    .map {
                        val name = it.select("module").text().split("(")[0]
                        val subTitle = it.select("room").text()
                        val day = DayOfWeek.of((1 + days.indexOf(date)))
                        //val staff = element.select("staff").text()
                        val startTimeInt = it.select("starttime").text().split(":").map { a -> a.toInt() }
                        val endTimeInt = it.select("endtime").text().split(":").map { a -> a.toInt() }
                        val color = Color.parseColor("#" + it.attr("colour"))
                        val startTime = LocalTime.of(startTimeInt[0], startTimeInt[1])
                        val endTime = LocalTime.of(endTimeInt[0], endTimeInt[1])
                        createEntry(name, subTitle, day, startTime, endTime, color)
                    }
            }
            .toList();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createEntry(name: String, subTitle: String, day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, color: Int): Event.Single {
        return Event.Single(
            id = random.nextLong(),
            date = LocalDate.now().with(day),
            title = name,
            shortTitle = name,
            subTitle = subTitle,
            timeSpan = TimeSpan(startTime, endTime),
            textColor = Color.BLACK,
            backgroundColor = color
        )
    }

    private fun randomColor(): Int {
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }

}