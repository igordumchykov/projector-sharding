package com.jdum.projector.sharding

import java.io.File
import kotlin.random.Random

fun main() {
    var stmtInsert = "insert into books (id, category_id, title) values \n"
    val resultInsert = (1..1000000).map {
        "(${it}, ${Random.nextInt(1, 20)}, 'title-${it}')"
    }.toMutableList()
    resultInsert.add("(1000001, 19, 'title-1_000_001');")
    stmtInsert += resultInsert.joinToString(",\n")

    File("data_insert.sql").printWriter().use { out ->
        out.println(stmtInsert)
    }

    val resultSelect = (1..1000001 step 100).map {
        "select * from books where id = $it"
    }.toMutableList()

    val stmtSelect = resultSelect.joinToString(";\n")
    File("data_select.sql").printWriter().use { out ->
        out.println(stmtSelect)
    }
    println()
}
