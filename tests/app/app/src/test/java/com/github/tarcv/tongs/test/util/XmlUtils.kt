package com.github.tarcv.tongs.test.util

import org.w3c.dom.CharacterData
import org.w3c.dom.Node

fun Node.attributeNamed(name: String): Node = attributes.getNamedItem(name)

fun Node.childrenAssertingNoText(): List<Node> {
    val nodes = ArrayList<Node>(childNodes.length)
    for (i in 0 until childNodes.length) {
        nodes.add(childNodes.item(i))
    }

    return nodes
            .filter {
                if (it is CharacterData) {
                    assert(it.nodeValue.isNullOrBlank())
                    assert(it.data.isNullOrBlank())
                    false
                } else {
                    true
                }
            }
}