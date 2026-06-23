package com.parvez.privacyisland

object IslandBus {
    private val listeners = linkedSetOf<(IslandEvent) -> Unit>()

    fun send(event: IslandEvent) {
        listeners.toList().forEach { it(event) }
    }

    fun subscribe(listener: (IslandEvent) -> Unit) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: (IslandEvent) -> Unit) {
        listeners.remove(listener)
    }
}
