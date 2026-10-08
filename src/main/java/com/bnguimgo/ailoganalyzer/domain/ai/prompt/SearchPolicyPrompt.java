package com.bnguimgo.ailoganalyzer.domain.ai.prompt;

/**
 * Cette classe explique au LLM comment rechercher
 */
public class SearchPolicyPrompt implements PromptSection {

    @Override
    public String build() {

        StringBuilder prompt = new StringBuilder();

        prompt.append("=== RÈGLE D'UTILISATION DE SEARCH_LOG ===\n");

        prompt.append("Utilise search_log uniquement lorsqu'une information nécessaire au diagnostic ")
                .append("n'est pas suffisamment démontrée par le contexte structuré fourni.\n");

        prompt.append("Avant chaque recherche, identifie explicitement ce que tu cherches à démontrer ")
                .append("ou à réfuter et choisis un terme de recherche aussi discriminant que possible : ")
                .append("privilégie une exception, un message d'erreur, une classe, une méthode ou un ")
                .append("élément technique directement lié à l'hypothèse plutôt qu'un terme générique.\n");

        prompt.append("Après chaque recherche, évalue explicitement le résultat par rapport à l'hypothèse : ")
                .append("CONFIRMÉE, INFIRMÉE ou INSUFFISANTE. ")
                .append("Si l'hypothèse est infirmée, réévalue le diagnostic avant toute nouvelle recherche.\n");

        prompt.append("Si une preuve suffisante est obtenue, considère cette branche du diagnostic comme ")
                .append("terminée et passe à la conclusion. Ne poursuis pas la recherche pour obtenir ")
                .append("davantage de contexte ou pour confirmer des détails secondaires qui ne peuvent ")
                .append("pas modifier le diagnostic.\n");

        prompt.append("N'effectue une nouvelle recherche que si une information précise manque encore ")
                .append("et que son résultat pourrait réellement confirmer, infirmer ou modifier ")
                .append("la conclusion actuelle. Si la recherche ne peut qu'apporter une confirmation ")
                .append("supplémentaire sans modifier le diagnostic, arrête les recherches.\n");

        prompt.append("Si une recherche précédente a déjà montré qu'une information recherchée n'est pas ")
                .append("présente dans le log, considère cette piste comme épuisée et ne la recherche pas ")
                .append("à nouveau.\n");

        prompt.append("Si la preuve nécessaire n'est pas disponible dans les résultats obtenus et ")
                .append("qu'aucune recherche supplémentaire raisonnable ne peut fournir cette preuve ")
                .append("avec les outils disponibles, considère cette information comme indisponible et ")
                .append("arrête les recherches.\n");

        prompt.append("Lorsque les preuves disponibles sont suffisantes pour répondre à la question, ")
                .append("arrête les recherches et produis directement l'analyse finale.\n\n");

        return prompt.toString();
    }
}