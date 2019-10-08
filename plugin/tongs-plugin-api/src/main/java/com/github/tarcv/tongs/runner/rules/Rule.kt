package com.github.tarcv.tongs.runner.rules

interface RuleFactory<in C, out R> {
    fun create(context: C): R
}
