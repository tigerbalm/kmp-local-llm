package com.thinq.kmp.sample.model

enum class PresetPrompt(val displayName: String, val promptTemplate: String) {
    SUMMARIZE(
        "Summarize",
        "Summarize the following text in a concise manner:\n\n"
    ),
    REWRITE(
        "Rewrite",
        "Rewrite the following text to make it clearer and more professional:\n\n"
    ),
    PROOFREAD(
        "Proofread",
        "Proofread the following text and correct any grammar or spelling errors:\n\n"
    ),
    EXPLAIN(
        "Explain",
        "Explain the following concept in simple terms:\n\n"
    )
}
