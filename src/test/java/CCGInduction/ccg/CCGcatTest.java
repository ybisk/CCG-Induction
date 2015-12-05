package CCGInduction.ccg;

import junit.framework.TestCase;

public class CCGcatTest extends TestCase {

  /**
   * Verbatim movement of Julia's CCGcat testsuites into a dummy JUnit test.    TODO:  Make actual unit tests
   * @throws Exception
   */
  public void testMain() throws Exception {
    //testsuiteRelPronCCGbank();
    //testsuite();
    //testsuiteInducedCats();
    assert(true);
  }
  /**
   * A testsuite for different kinds of lexical categories.
   */
  public static void testsuite() {
    System.out.println("========================================");
    System.out.println("   TEST SUITE FOR LEXICAL CATEGORIES   ");
    System.out.println("========================================\n");


    CCGcat a = lexCat("eat", "S/N");
    CCGcat b = lexCat("very", "(N/N)/(N/N)");
    System.out.println("\"eat very\" compose S/N (N/N)/(N/N) => (S/N)/(N/N)");
    CCGcat c = compose(a, b);
    c.printCat();
    CCGcat d = lexCat("spicy", "N/N");
    CCGcat e = lexCat("food", "N");
    CCGcat f = apply(c, d);
    System.out.print("\" eat very spicy\" apply (S/N)/(N/N) N/N => S/N: ");
    f.printCat();
    CCGcat g = apply(f, e);
    System.out.print("\" eat very spicy food\" apply S/N N => S: ");
    g.printCat();

    // C
    CCGcat conj = lexCat("but", "conj");
    // conj.printCat();
    CCGcat a2 = lexCat("cook", "S/N");
    // a2.printCat();
    CCGcat b2 = lexCat("rather", "(N/N)/(N/N)");
    // b2.printCat();
    CCGcat c2 = compose(a2, b2);
    // c2.printCat();

    // FW_CONJOIN
    CCGcat CCright = conjunction(c2, conj, DepType.CCG);
    System.out.print("@@@ (S/N)/(N/N)[conj] (but cook rather): ");
    CCright.printCat();
    // BW_CONJOIN
    System.out.println("@@@ \"eat very  but cook rather\" :");
    // DEBUG = true;
    CCGcat out = coordinate(c, CCright, DepType.CCG);

    System.out.print("(S/N)/(N/N) -> (S/N)/(N/N) (S/N)/(N/N)[conj] (eat very  but cook rather): ");
    out.printCat();
    CCGcat x = apply(out, d);
    System.out.print("@@@ eat very but cook rather spicy: ");
    x.printCat();
    // DEBUG = false;
    CCGcat y = apply(x, e);
    System.out.print("@@@ eat very but cook rather spicy food: ");
    y.printCat();

    // CCGcat a = lexCat("VBZ", "(S/N)/N");
    // CCGcat b = lexCat("VBN", "S\\(S/N)");
    // CCGcat c = compose(b,a);
    // c.printCat();
    // c.printFilledDeps();
    // CCGcat d = lexCat("RB", "S/S");
    // CCGcat e = compose(d, c);
    // e.printCat();
    // e.printFilledDeps();
    // CCGcat f = lexCat("X", "(S\\N)/(S/N)");
    // CCGcat g = apply(f, e);
    // g.printCat();
    // g.printFilledDeps();
    // System.exit(0);

    CCGcat likes = lexCat("likes", "(S[dcl]\\NP)/(S[ng]\\NP)");

    CCGcat sleeps = lexCat("sleeps", "S[dcl]\\NP");
    CCGcat sleeping = lexCat("sleeping", "S[ng]\\NP");
    CCGcat dreaming = lexCat("dreaming", "S[ng]\\NP");
    // sleeps.printCat();
    CCGcat gives = lexCat("gives", "((S[dcl]\\NP)/NP)/NP");
    CCGcat writes = lexCat("writes", "(S[dcl]\\NP)/NP");
    CCGcat reading = lexCat("reading", "(S[ng]\\NP)/NP");
    CCGcat reads = lexCat("reads", "(S[dcl]\\NP)/NP");
    // gives.printCat();
    CCGcat john = lexCat("John", NP);
    CCGcat john2 = john.typeRaise(S, FW);
    CCGcat mary = lexCat("Mary", NP);
    CCGcat mary2 = mary.typeRaise("(S\\NP)/NP", BW);
    CCGcat mary3 = mary.typeRaise(S, FW);
    CCGcat sue = lexCat("Sue", NP);
    CCGcat sue2 = sue.typeRaise("(S\\NP)/NP", BW);
    CCGcat flowers = lexCat("flowers", NP);
    CCGcat flowers2 = flowers.typeRaise(VP, BW);
    CCGcat books = lexCat("books", NP);
    CCGcat books2 = books.typeRaise(VP, BW);

    // System.out.println();
    CCGcat well = lexCat("well", "(S\\NP)\\(S\\NP)");
    // System.out.println(well.print());
    CCGcat very1 = lexCat("very", "(N/N)/(N/N)");
    CCGcat very = lexCat("very", "((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP))");
    System.out.println(very1.print());
    // System.out.println(very.print());
    // System.exit(0);

    CCGcat fast = lexCat("fast", "(S\\NP)\\(S\\NP)");
    CCGcat happily = lexCat("happily", "(S\\NP)\\(S\\NP)");

    CCGcat on = lexCat("on", "(NP\\NP)/NP");
    CCGcat without = lexCat("without", "((S\\NP)\\(S\\NP))/(S[ng]\\NP)");

    // Configuration.CCGcat_CoNLL = true;
    CCGcat and = lexCat("and", "conj");

    System.out.println("conjunction (FW_CONJOIN) - conjunction and 2nd conjunct");
    CCGcat andMary = conjunction(mary, and, DepType.CCG);
    andMary.printCat();
    System.out.println("coordinate (BW_CONJOIN) - 1st conjunct and conjunction");
    CCGcat johnmary = coordinate(andMary, john, DepType.CCG);
    johnmary.printCat();
    // System.exit(0);
    System.out.println("STANDARD FUNCTION APPLICATION");
    System.out.println("=============================");
    System.out.println("apply(sleeps:" + sleeps.catString + ", john:"
        + john.catString + ')');
    CCGcat johnsleeps = apply(sleeps, john);
    johnsleeps.printCat();
    System.out.println();
    System.out.println("FUNCTION APPLICATION WITH TYPE-RAISED SUBJECT");
    System.out.println("=============================================");
    System.out.println("apply(john:" + john2.catString + ", sleeps:"
        + sleeps.catString + ')');
    CCGcat john2sleeps = apply(john2, sleeps);
    john2sleeps.printCat();
    System.out.println();
    System.out.println("FUNCTION APPLICATION WITH NP-ADJUNCT");
    System.out.println("=====================================");
    System.out.println("apply(on:" + on.catString + ",  flowers:NP)");
    CCGcat onflowers = apply(on, flowers);
    onflowers.printCat();
    System.out.println("apply(onFlowers:NP\\NP, books:NP)");
    CCGcat booksOnFlowers = apply(onflowers, books);
    booksOnFlowers.printCat();

    System.out.println("apply(reads:" + reads.catString
        + ", booksOnFlowers:" + booksOnFlowers.catString + ')');
    CCGcat readsBooksOnFlowers = apply(reads, booksOnFlowers);
    readsBooksOnFlowers.printCat();

    System.out.println();
    System.out.println("FUNCTION APPLICATION WITH VP-ADJUNCT");
    System.out.println("=============================");
    // System.out.println("apply(well:"+ well.catString
    // +", sleeps:"+sleeps.catString+")");
    CCGcat sleepsWell = apply(well, sleeps);
    sleepsWell.printCat();
    System.out.println("apply(sleepsWell:" + sleepsWell.catString
        + ", john:" + john.catString + ')');
    CCGcat johnsleepsWell = apply(sleepsWell, john);
    johnsleepsWell.printCat();
    System.out.println("apply(very:" + very.catString + ", well:"
        + well.catString + ')');
    CCGcat veryWell = apply(very, well);
    veryWell.printCat();

    System.out.println("apply(veryWell:" + veryWell.catString + ", sleeps:"
        + sleeps.catString + ')');
    CCGcat sleepsVeryWell = apply(veryWell, sleeps);
    sleepsVeryWell.printCat();

    System.out.println("apply(sleepsVeryWell:" + sleepsVeryWell.catString
        + ", john:" + john.catString + ')');
    CCGcat johnsleepsVeryWell = apply(sleepsVeryWell, john);
    johnsleepsVeryWell.printCat();
    System.out.println("compose(veryWell:" + veryWell.catString
        + ", reads:" + reads.catString);

    CCGcat readsVeryWell = compose(veryWell, reads);
    readsVeryWell.printCat();

    // flowers2.printCat();
    System.out.println();
    System.out.println("COMPOSITION OF ARGUMENTS");
    System.out.println("========================");
    System.out.println("compose(flowers:" + flowers2.catString + ", mary:"
        + mary2.catString + ')');

    CCGcat maryflowers = compose(flowers2, mary2);
    maryflowers.printCat();

    // System.out.println();
    System.out.println("apply(maryflowers:" + maryflowers.catString
        + ", gives:" + gives.catString + ')');
    CCGcat givesmaryflowers = apply(maryflowers, gives);
    givesmaryflowers.printCat();

    System.out.println("apply(john:" + john2.catString
        + ", givesmaryflowers:" + givesmaryflowers.catString + ')');
    CCGcat john2givesmaryflowers = apply(john2, givesmaryflowers);
    john2givesmaryflowers.printCat();
    System.out.println();
    System.out.println("COMPOSITION WITH VP-ADJUNCT");
    System.out.println("============================");
    CCGcat with = lexCat("with", "((S\\NP)\\(S\\NP))/NP");
    // System.out.println();
    System.out.println("apply(with:" + with.catString + ", john:"
        + john.catString + ')');
    CCGcat withJohn = apply(with, john);
    // System.out.println();
    System.out.println("compose(withJohn:" + withJohn.catString
        + ", reading:" + reading.catString + ')');
    CCGcat readingwithJohn = compose(withJohn, reading);
    readingwithJohn.printCat();
    System.out.println();

    System.out.println("VP COORDINATION");
    System.out.println("===============");
    System.out.println("coordinate(writes" + writes.catString + ", reads"
        + reads.catString + ')');
    CCGcat writesAndreads = coordinate(writes, reads, DepType.CCG);
    writesAndreads.printCat();

    // System.out.println();
    System.out.println("apply(writesAndreads:" + writesAndreads.catString
        + ", books:" + books.catString + ')');
    CCGcat writesAndreadsbooks = apply(writesAndreads, books);
    writesAndreadsbooks.printCat();
    System.out.println("compose(john:" + john2.catString
        + ", writesandreads:" + writesAndreads.catString + ')');

    CCGcat johnwritesandreads = compose(john2, writesAndreads);
    johnwritesandreads.printCat();

    // System.out.println();
    System.out.println("apply(johnWritesAndReads:"
        + johnwritesandreads.catString + ", books:" + books.catString
        + ')');

    CCGcat johnwritesandreadsbooks = apply(johnwritesandreads, books);
    johnwritesandreadsbooks.printCat();
    System.out.println();
    System.out.println("ARGUMENT CLUSTER COORDINATION");
    System.out.println("=============================");
    System.out.println("compose(books:" + books2.catString + ", sue:"
        + sue2.catString + ')');

    CCGcat suebooks = compose(books2, sue2);
    System.out.println("coordinate(SueBooks, MaryFlowers)");
    CCGcat suebooksandmaryflowers = coordinate(suebooks, maryflowers, DepType.CCG);
    suebooksandmaryflowers.printCat();
    System.out.println("apply(SueBooksAndMaryFlowers:"
        + suebooksandmaryflowers.catString + ", gives:"
        + gives.catString + ')');
    CCGcat givessuebooksandmaryflowers = apply(suebooksandmaryflowers,
        gives);
    givessuebooksandmaryflowers.printCat();
    System.out.println();
    System.out.println("COORDINATION OF ADJUNCTS");
    System.out.println("=========================");
    System.out.println("coordinate(well:" + well.catString + ", fast:"
        + fast.catString + ')');
    CCGcat wellandFast = coordinate(well, fast, DepType.CCG);
    // wellandFast.printCat();
    System.out.println("compose(wellAndFast:" + wellandFast.catString
        + ", writes:" + writes.catString + ')');
    CCGcat writeswellandFast = compose(wellandFast, writes);
    writeswellandFast.printCat();
    System.out.println();
    System.out.println("NP COORDINATION");
    System.out.println("===============");
    System.out.println("coordinate(john:NP, mary:NP)");
    CCGcat johnmary2 = coordinate(john, mary, DepType.CCG);
    johnmary2.printCat();
    System.out.println("apply(writesAndReadsBooks:"
        + writesAndreadsbooks.catString + ", johnandmary:"
        + johnmary.catString + ')');
    // writesAndreadsbooks.printCat();
    CCGcat johnmarywritesandreadsbooks = apply(writesAndreadsbooks,
        johnmary);
    johnmarywritesandreadsbooks.printCat();
    System.out.println();
    System.out.println("ARGUMENT PASSING: VP-ADJUNCTS");
    System.out.println("============================");

    System.out.println("apply(without:" + without.catString + ", dreaming:"
        + dreaming.catString + ')');
    CCGcat withoutdreaming = apply(without, dreaming);
    withoutdreaming.printCat();

    System.out.println("apply(withoutDreaming:" + withoutdreaming.catString
        + ", sleeps:" + sleeps.catString + ')');
    CCGcat sleepsWithoutDreaming = apply(withoutdreaming, sleeps);
    sleepsWithoutDreaming.printCat();
    System.out.println("apply(sleepsWithoutDreaming:"
        + sleepsWithoutDreaming.catString + ", mary:" + mary.catString
        + ')');
    CCGcat marySleepsWithoutDreaming = apply(sleepsWithoutDreaming, mary);
    marySleepsWithoutDreaming.printCat();

    System.out.println();
    System.out.println("PARASITIC GAPS: SUBSTITUTION");
    System.out.println("============================");
    System.out.println("compose(without:" + without.catString
        + ",  reading:" + reading.catString + ')');

    CCGcat withoutreading = compose(without, reading);
    withoutreading.printCat();

    System.out.println("substitute(without_reading:"
        + withoutreading.catString + ", writes:" + writes.catString
        + ')');

    CCGcat writeswithoutreading = substitute(withoutreading, writes, DepType.CCG);
    writeswithoutreading.printCat();

    System.out.println("apply(writesWithoutreading:"
        + writeswithoutreading.catString + ", books:" + books.catString
        + ')');
    CCGcat writeswithoutreadingbooks = apply(writeswithoutreading, books);
    writeswithoutreadingbooks.printCat();
    System.out.println("apply(writesWithoutreadingbooks:"
        + writeswithoutreading.catString + ", mary:" + mary.catString
        + ')');
    CCGcat marywriteswithoutreadingbooks = apply(writeswithoutreadingbooks,
        mary);
    marywriteswithoutreadingbooks.printCat();

    System.out.println();
    System.out.println("RELATIVE PRONOUNS -- OBJECT EXTRACTION");
    System.out.println("======================================");

    CCGcat which = lexCat("which", "(NP\\NP)/(S/NP)");
    System.out.println("Lexical entry for \"which\":");
    which.printCat();
    CCGcat johnwrites = compose(john2, writes);
    System.out.println("The category for \"John writes\":");
    johnwrites.printCat();
    CCGcat whichjohnwrites = apply(which, johnwrites);
    System.out.println("The category for \"which John writes\":");
    whichjohnwrites.printCat();
    // System.out.println();
    System.out.println("apply(which:" + which.catString
        + ", johnwritesandreads:" + johnwritesandreads.catString + ')');
    CCGcat whichJohnWritesAndReads = apply(which, johnwritesandreads);
    whichJohnWritesAndReads.printCat();

    System.out.println();
    System.out.println("apply(whichJohnWritesAndReads:"
        + whichJohnWritesAndReads.catString + ", books:"
        + books.catString + ')');
    CCGcat booksWhichJohnWritesAndReads = apply(whichJohnWritesAndReads,
        books);
    booksWhichJohnWritesAndReads.printCat();
    System.out.println("apply(reads:" + reads.catString
        + ", booksWhichJohnWritesAndReads:"
        + booksWhichJohnWritesAndReads.catString + ')');

    CCGcat readsbooksWhichJohnWritesAndReads = apply(reads,
        booksWhichJohnWritesAndReads);
    readsbooksWhichJohnWritesAndReads.printCat();
    System.out.println();
    System.out.println("RELATIVE PRONOUNS & SBJ CONTROL       ");
    System.out.println("======================================");

    // likes is recognised as adjunct category, which is wrong!

    System.out.println("compose(likes:" + likes.catString + ", reading:"
        + reading.catString + ')');
    CCGcat likesreading = compose(likes, reading);
    likesreading.printCat();
    System.out.println("compose(mary:" + mary3.catString
        + ", likesReading:" + likesreading.catString + ')');
    CCGcat marylikesreading = compose(mary3, likesreading);
    marylikesreading.printCat();
    System.out.println("apply(which:" + which.catString
        + ", MarylikesReading:" + marylikesreading.catString + ')');
    CCGcat whichmarylikesreading = apply(which, marylikesreading);
    whichmarylikesreading.printCat();
    System.out.println("apply(whichMaryLikesReading:"
        + whichmarylikesreading.catString + ", books" + books.catString
        + ')');
    CCGcat bookswhichmarylikesreading = apply(whichmarylikesreading, books);
    bookswhichmarylikesreading.printCat();
    System.out.println("apply(writes:" + writes.catString
        + ", booksWhichMaryLikesReading:"
        + bookswhichmarylikesreading.catString + ')');
    CCGcat writesbookswhichmarylikesreading = apply(writes,
        bookswhichmarylikesreading);
    writesbookswhichmarylikesreading.printCat();
    System.out.println("compose(well, fast):");
    CCGcat wellfast = compose(well, fast);
    // System.out.println("EXIT compose(well, fast):");

    wellfast.printCat();
    System.out.println("compose(wellfast, happily):");
    CCGcat wellfasthappily = compose(wellfast, happily);
    // System.out.println("EXIT compose(wellfast, happily):");
    wellfasthappily.printCat();
    System.out.println("compose(wellfast, writes)");
    CCGcat writeswellfast = compose(wellfast, writes);
    // System.out.println("EXIT compose(wellfast, writes)");
    writeswellfast.printCat();
    System.out.println("compose(wellfasthappily, writes)");
    CCGcat writeswellfasthappily = compose(wellfasthappily, writes);
    // System.out.println("EXIT compose(wellfasthappily, writes)");
    writeswellfasthappily.printCat();
    System.out.println("apply(wellfasthappily, sleeping)");
    CCGcat sleepingwellfasthappily = apply(wellfasthappily, sleeping);
    // System.out.println("EXIT apply(wellfasthappily, sleeping)");
    sleepingwellfasthappily.printCat();
    System.out.println("apply(likes, sleepingwellfasthappily)");
    CCGcat likessleepingwellfasthappily = apply(likes,
        sleepingwellfasthappily);
    // System.out.println("EXIT apply(likes, sleepingwellfasthappily)");
    likessleepingwellfasthappily.printCat();
    System.out.println("apply(likessleepingwellfasthappily, john)");
    CCGcat johnlikessleeping2 = apply(likessleepingwellfasthappily, john);
    // System.out.println("EXIT apply(likessleepingwellfasthappily, john)");
    johnlikessleeping2.printCat();

    CCGcat likessleeping = apply(likes, sleeping);
    likessleeping.printCat();

    // System.out.println("EXIT apply(likes, sleeping)");
    CCGcat johnlikessleeping = apply(likessleeping, john);
    johnlikessleeping.printCat();

    CCGcat veryN = lexCat("very", "(N/N)/(N/N)");
    // veryN.printCat();
    CCGcat big = lexCat("BIG", "N/N");
    // BIG.printCat();
    CCGcat red = lexCat("red", "N/N");
    // red.printCat();
    CCGcat book = lexCat("book", NOUN);
    System.out.println();
    System.out.println("COMPOSITION OF N-MODIFIERS:");
    System.out.println("===========================");
    System.out.println("compose(BIG, red):");
    CCGcat bigred = compose(big, red);
    bigred.printCat();
    System.out.println("apply(very, bigred):");
    CCGcat verybigred = apply(veryN, bigred);
    verybigred.printCat();
    System.out.println("apply(verybigred, book):");
    CCGcat verybigredbook = apply(verybigred, book);
    verybigredbook.printCat();

    System.out.println();
    System.out.println("COMPOSITION OF VP-MODIFIERS:");
    System.out.println("===========================");
    System.out.println("compose(well, fast):");
    CCGcat wellfastB = compose(well, fast);
    wellfastB.printCat();
    System.out.println("apply(very, wellfast):");
    CCGcat verywellfast = apply(very, wellfastB);
    verywellfast.printCat();
    System.out.println("apply(verywellfast, sleep):");
    CCGcat sleepverywellfast = apply(verywellfast, sleeps);
    sleepverywellfast.printCat();

  }

  public static void testsuiteRelPronCCGbank() {
    System.out.println("========================================");
    System.out.println("   TEST SUITE FOR RELATIVE PRONOUNS");
    System.out.println("========================================\n");
    CCGcat sleeps = lexCat("sleeps", "S\\N");
    CCGcat writes = lexCat("writes", "(S\\N)/N");
    CCGcat likes = lexCat("likes", "(S\\N)/(S\\N)");
    CCGcat reading = lexCat("reading", "(S\\N)/N");
    CCGcat reads = lexCat("reads", "(S\\N)/N");
    CCGcat john = lexCat("John", NOUN);
    CCGcat john2 = john.typeRaise(S, FW);
    CCGcat mary = lexCat("Mary", NOUN);
    CCGcat mary3 = mary.typeRaise(S, FW);
    CCGcat books = lexCat("books", NOUN);

    System.out.println("STANDARD FUNCTION APPLICATION");
    System.out.println("=============================");
    System.out.println("\"John sleeps\": apply(sleeps:" + sleeps.catString + ", john:"
        + john.catString + ')');
    CCGcat johnsleeps = apply(sleeps, john);
    johnsleeps.printCat();
    System.out.println();
    System.out.println("FUNCTION APPLICATION WITH TYPE-RAISED SUBJECT");
    System.out.println("=============================================");
    System.out.println("\"John sleeps\": apply(john:" + john2.catString + ", sleeps:"
        + sleeps.catString + ')');
    CCGcat john2sleeps = apply(john2, sleeps);
    john2sleeps.printCat();
    System.out.println();
    System.out.println();
    System.out.println("RELATIVE PRONOUNS -- OBJECT EXTRACTION");
    System.out.println("======================================");
    CCGcat which = lexCat("which", "(N\\N)/(S/N)");
    System.out.println("Lexical entry for \"which\":");
    which.printCat();
    System.out.println("Compose \"John\" and \"writes\":");
    CCGcat johnwrites = compose(john2, writes);
    System.out.println("The category for \"John writes\":");
    johnwrites.printCat();
    System.out.println("Apply \"which\" to \"John writes\":");
    CCGcat whichjohnwrites = apply(which, johnwrites);
    System.out.println("The category for \"which John writes\":");
    System.out.println("Should be N\\N where the argument depends on \"which\" and on \"writes\"");
    whichjohnwrites.printCat();
    System.out.println();

    System.out.println("apply(whichJohnWrites:"
        + whichjohnwrites.catString + ", books:"
        + books.catString + ')');
    CCGcat booksWhichJohnWrites = apply(whichjohnwrites,
        books);
    System.out.println("DEBUG: do we see (books, writes) and (books, with)?");
    booksWhichJohnWrites.printCat();
    System.out.println("apply(reads:" + reads.catString
        + ", booksWhichJohnWrites:"
        + booksWhichJohnWrites.catString + ')');

    CCGcat readsbooksWhichJohnWrites = apply(reads,
        booksWhichJohnWrites);
    readsbooksWhichJohnWrites.printCat();
    System.out.println();
    System.out.println("RELATIVE PRONOUNS & SBJ EXTRACTION       ");
    System.out.println("======================================");

    // likes is recognised as adjunct category, which is wrong!

    System.out.println("compose(likes:" + likes.catString + ", reading:"
        + reading.catString + ')');
    CCGcat likesreading = compose(likes, reading);
    likesreading.printCat();
    System.out.println("compose(mary:" + mary3.catString
        + ", likesReading:" + likesreading.catString + ')');
    CCGcat marylikesreading = compose(mary3, likesreading);
    marylikesreading.printCat();
    System.out.println("apply(which:" + which.catString
        + ", MarylikesReading:" + marylikesreading.catString + ')');
    CCGcat whichmarylikesreading = apply(which, marylikesreading);
    whichmarylikesreading.printCat();
    System.out.println("apply(whichMaryLikesReading:"
        + whichmarylikesreading.catString + ", books" + books.catString
        + ')');
    CCGcat bookswhichmarylikesreading = apply(whichmarylikesreading, books);
    bookswhichmarylikesreading.printCat();
    System.out.println("apply(writes:" + writes.catString
        + ", booksWhichMaryLikesReading:"
        + bookswhichmarylikesreading.catString + ')');
    CCGcat writesbookswhichmarylikesreading = apply(writes,
        bookswhichmarylikesreading);
    writesbookswhichmarylikesreading.printCat();

  }

  /**
   * Test suite to make sure that non-local dependencies in induced categories
   * that may or may not look like English CCGbank categories get treated
   * correctly. Case 1: non-local dependencies in relative pronoun type
   * categories Case 2: auxiliary verbs Case 3: type-changing rules
   */
  public static void testsuiteInducedCats() {
    //System.out.println("COMPARISON WITH CCGBANK");
    CCGcat sleepsCCGbank = lexCat("sleepsWSJ", "S\\N");
    //sleepsCCGbank.printCat();
    CCGcat whoCCGbank = lexCat("whoWSJ", "(N\\N)/(S\\N)");
    whoCCGbank.printCat();
    System.out.println("N\\N who sleeps (CCGbank):");
    CCGcat whosleepsCCGbank = apply(whoCCGbank, sleepsCCGbank);
    whosleepsCCGbank.printCat();
    CCGcat johnCCGbank = lexCat("JohnWSJ", "N");
    System.out.println("N john who sleeps (CCGbank):");
    CCGcat johnwhosleepsCCGbank = apply(whosleepsCCGbank, johnCCGbank);
    johnwhosleepsCCGbank.printCat();
    System.out.println("------------------");
    System.out.println("\n================================");
    System.out.println("TESTSUITE FOR INDUCED CATEGORIES");
    System.out.println("================================\n");
    // Inventory of atomic categories and auxiliaries:
    CCGcat john = lexCat("John", NOUN);
    CCGcat sleeps = lexCat("sleeps", "S\\N");
    //sleeps.printCat();
    //System.out.println("John sleeps (dummy sentence):");
    System.out.println();
    System.out.println("Test 1: Relative pronouns");
    System.out.println("-------------------------");
    // Inventory of relative-pronoun-like categories:
    CCGcat who1sbj = lexCat("who1sbj", "(N\\N)/(S\\N)");
    //		CCGcat who3sbj = lexCat("who3sbj", "(N\\N)\\(S\\N)");
    //		CCGcat who4sbj = lexCat("who4sbj", "(N/N)\\(S\\N)");
    //		CCGcat who1obj = lexCat("who1obj", "(N\\N)/(S\\N)");
    //		CCGcat who2obj = lexCat("who2obj", "(N/N)/(S\\N)");
    //		CCGcat who3obj = lexCat("who3obj", "(N\\N)\\(S\\N)");
    //		CCGcat who4obj = lexCat("who4obj", "(N/N)\\(S\\N)");
    System.out.println("case 1: " + who1sbj);
    System.out.println("N\\N  who sleeps:");
    CCGcat whosleeps1 = apply(who1sbj, sleeps);
    whosleeps1.printCat();
    whosleeps1.printFilledCCGDeps(DepType.CCG);
    System.out.println();
    System.out.println("N john who sleeps:");
    CCGcat johnwhosleeps1 = apply(whosleeps1, john);
    johnwhosleeps1.printCat();
    johnwhosleeps1.printFilledCCGDeps(DepType.CCG);
    System.out.println();
    /*
     * System.out.println("case 2: " + who1sbj); CCGcat whosleeps2 =
     * apply(who2sbj, sleeps); whosleeps2.printCat();
     * whosleeps2.printFilledCCGDeps(); CCGcat johnwhosleeps2 =
     * apply(whosleeps2, john); johnwhosleeps2.printCat();
     * johnwhosleeps2.printFilledCCGDeps(); System.out.println("case 3: " +
     * who1sbj); CCGcat whosleeps3 = apply(who3sbj, sleeps);
     * whosleeps3.printCat(); whosleeps3.printFilledCCGDeps(); CCGcat
     * johnwhosleeps3 = apply(whosleeps3, john); johnwhosleeps3.printCat();
     * johnwhosleeps3.printFilledCCGDeps(); System.out.println("case 4: " +
     * who4sbj); CCGcat whosleeps4 = apply(who4sbj, sleeps);
     * whosleeps4.printCat(); whosleeps4.printFilledCCGDeps(); CCGcat
     * johnwhosleeps4 = apply(whosleeps4, john); johnwhosleeps4.printCat();
     * johnwhosleeps4.printFilledCCGDeps(); // Auxiliaries CCGcat aux1 =
     * lexCat("aux1", "(S\\N)/(S\\N)"); CCGcat aux2 = lexCat("aux2",
     * "(S\\N)/(S/N)");
     */

  }
}