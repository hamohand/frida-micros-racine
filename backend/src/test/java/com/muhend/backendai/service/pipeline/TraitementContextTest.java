package com.muhend.backendai.service.pipeline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraitementContextTest {

    @Test
    void testIncrementCounters() {
        TraitementContext ctx = new TraitementContext();

        // Vérifier l'état initial
        assertEquals(0, ctx.getNbConjoints());
        assertEquals(0, ctx.getNbGarcons());
        assertEquals(0, ctx.getNbFilles());
        assertEquals(0, ctx.getNbParents());
        assertEquals(0, ctx.getNbFreres());
        assertEquals(0, ctx.getNbSoeurs());

        // Incrémenter les compteurs
        ctx.incrementConjoints();
        ctx.incrementConjoints();

        ctx.incrementGarcons();
        ctx.incrementGarcons();
        ctx.incrementGarcons();

        ctx.incrementFilles();
        
        ctx.incrementParents("ذكر");
        ctx.incrementParents("انثى");

        ctx.incrementFreres();
        ctx.incrementSoeurs();

        // Valider l'état après modifications
        assertEquals(2, ctx.getNbConjoints());
        assertEquals(3, ctx.getNbGarcons());
        assertEquals(1, ctx.getNbFilles());
        assertEquals(2, ctx.getNbParents());
        assertEquals(1, ctx.getNbFreres());
        assertEquals(1, ctx.getNbSoeurs());
    }

    @Test
    void testInitialListsAreNotNull() {
        TraitementContext ctx = new TraitementContext();

        assertNotNull(ctx.getTableauNumParente());
        assertNotNull(ctx.getListeHeritiers());
        assertNotNull(ctx.getListeTemoins());
        
        assertTrue(ctx.getTableauNumParente().isEmpty());
        assertTrue(ctx.getListeHeritiers().isEmpty());
        assertTrue(ctx.getListeTemoins().isEmpty());
    }
}
