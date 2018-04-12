package com.pnodder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import java.time.Instant;
import java.util.*;

@RunWith(Arquillian.class)
public class EntityManagerTester {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
                .addPackage(Event.class.getPackage())
                .addAsResource("test-persistence.xml", "META-INF/persistence.xml")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @PersistenceContext
    private EntityManager em;

    @Inject
    UserTransaction utx;

    @Before
    public void preparePersistenceTest() throws Exception {
        clearData();
        insertData();
        startTransaction();
    }

    @After
    public void commitTransaction() throws Exception {
        utx.commit();
    }

    private void clearData() throws Exception {
        utx.begin();
        em.joinTransaction();
        System.out.println("Dumping old records...");
        em.createQuery("delete from Game").executeUpdate();
        utx.commit();
    }

    private void insertData() throws Exception {
        utx.begin();
        em.joinTransaction();
        System.out.println("Inserting records...");
        em.persist(new Event("party", Date.from(Instant.now())));
        em.persist(new Event("rave", Date.from(Instant.now())));
        utx.commit();
        // clear the persistence context (first-level cache)
        em.clear();
    }

    private void startTransaction() throws Exception {
        utx.begin();
        em.joinTransaction();
    }

    @Test
    public void shouldFindAllGamesUsingJpqlQuery() throws Exception {
        // given
        String fetchingAllGamesInJpql = "select e from Event e order by e.id";

        // when
        System.out.println("Selecting (using JPQL)...");
        List<Event> games = em.createQuery(fetchingAllGamesInJpql, Event.class).getResultList();

        // then
        System.out.println("Found " + games.size() + " games (using JPQL):");
        assertContainsAllGames(games);
    }

    private static void assertContainsAllGames(Collection<Event> retrievedGames) {
        Assert.assertEquals(2, retrievedGames.size());
        final Set<String> retrievedGameTitles = new HashSet<>();
        for (Event game : retrievedGames) {
            System.out.println("* " + game);
            retrievedGameTitles.add(game.getTitle());
        }
    }

}
