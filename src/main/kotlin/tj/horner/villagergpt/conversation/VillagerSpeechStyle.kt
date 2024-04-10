package tj.horner.villagergpt.conversation

enum class VillagerSpeechStyle {
    MILDLY_MEDIEVAL {
        override fun promptDescription(): String =
                "Speak in a mildly medieval style"
    },
    CRYPTIC {
        override fun promptDescription(): String =
                "Speak in a cryptic and metaphorical style"
    },
    SOUTHERN {
        override fun promptDescription(): String =
                "Speak as though you are from the deep south "
    },
    FORMAL {
        override fun promptDescription(): String =
                "Speak in a formal and proper manner like royalty"
    },
    INFORMAL {
        override fun promptDescription(): String =
                "Speak in an informal and casual manner "
    },
    Straightforward {
        override fun promptDescription(): String =
                "Speak in a straightforward manner"
    },
    SLANG {
        override fun promptDescription(): String =
                "Speak using slang and colloquialisms as though you are a teenager"
    },
    PIRATE {
        override fun promptDescription(): String =
                "Speak like a pirate"
    };

    abstract fun promptDescription(): String
}