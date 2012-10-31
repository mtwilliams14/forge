package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class DigEffect extends SpellEffect {
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getAbilityFactory().getHostCard();
        final StringBuilder sb = new StringBuilder();
        final int numToDig = AbilityFactory.calculateAmount(host, params.get("DigNum"), sa);
    
        if (!(sa instanceof AbilitySub)) {
            sb.append(sa.getSourceCard()).append(" - ");
        } else {
            sb.append(" ");
        }
    
        if (params.containsKey("StackDescription")) {
            sb.append(params.get("StackDescription"));
            return sb.toString();
        }
    
        ArrayList<Player> tgtPlayers;
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        sb.append(host.getController()).append(" looks at the top ").append(numToDig);
        sb.append(" card");
        if (numToDig != 1) {
            sb.append("s");
        }
        sb.append(" of ");
        if (tgtPlayers.contains(host.getController())) {
            sb.append("his or her ");
        } else {
            for (final Player p : tgtPlayers) {
                sb.append(p).append("'s ");
            }
        }
        sb.append("library.");
        return sb.toString();
    }

    /**
     * <p>
     * digResolve.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card host = sa.getSourceCard();
        final Player player = sa.getActivatingPlayer();
        Player choser = player;
        int numToDig = AbilityFactory.calculateAmount(host, params.get("DigNum"), sa);
        final ZoneType destZone1 = params.containsKey("DestinationZone") ? ZoneType.smartValueOf(params.get("DestinationZone"))
                : ZoneType.Hand;
        final ZoneType destZone2 = params.containsKey("DestinationZone2") ? ZoneType.smartValueOf(params
                .get("DestinationZone2")) : ZoneType.Library;

        final int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params
                .get("LibraryPosition")) : -1;
        int destZone1ChangeNum = 1;
        final boolean mitosis = params.containsKey("Mitosis");
        String changeValid = params.containsKey("ChangeValid") ? params.get("ChangeValid") : "";
        //andOrValid is for cards with "creature card and/or a land card"
        String andOrValid = params.containsKey("AndOrValid") ? params.get("AndOrValid") : "";
        final boolean anyNumber = params.containsKey("AnyNumber");

        final int libraryPosition2 = params.containsKey("LibraryPosition2") ? Integer.parseInt(params
                .get("LibraryPosition2")) : -1;
        final boolean optional = params.containsKey("Optional");
        final boolean noMove = params.containsKey("NoMove");
        boolean changeAll = false;
        final ArrayList<String> keywords = new ArrayList<String>();
        if (params.containsKey("Keywords")) {
            keywords.addAll(Arrays.asList(params.get("Keywords").split(" & ")));
        }

        if (params.containsKey("ChangeNum")) {
            if (params.get("ChangeNum").equalsIgnoreCase("All")) {
                changeAll = true;
            } else {
                destZone1ChangeNum = Integer.parseInt(params.get("ChangeNum"));
            }
        }

        ArrayList<Player> tgtPlayers;

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else {
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }

        if (params.containsKey("Choser")) {
            final ArrayList<Player> chosers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),
                    params.get("Choser"), sa);
            if (!chosers.isEmpty()) {
                choser = chosers.get(0);
            }
        }

        for (final Player p : tgtPlayers) {
            if (tgt != null && !p.canBeTargetedBy(sa)) {
                continue;
            }
            final List<Card> top = new ArrayList<Card>();
            List<Card> valid = new ArrayList<Card>();
            final List<Card> rest = new ArrayList<Card>();
            final PlayerZone library = p.getZone(ZoneType.Library);

            numToDig = Math.min(numToDig, library.size());
            for (int i = 0; i < numToDig; i++) {
                top.add(library.get(i));
            }

            if (top.size() > 0) {
                final Card dummy = new Card();
                dummy.setName("[No valid cards]");

                if (params.containsKey("Reveal")) {
                    GuiChoose.one("Revealing cards from library", top);
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (params.containsKey("RevealOptional")) {
                    String question = "Reveal: ";
                    for (final Card c : top) {
                        question += c + " ";
                    }
                    if (p.isHuman() && GameActionUtil.showYesNoDialog(host, question)) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                        // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    } else if (p.isComputer() && (top.get(0).isInstant() || top.get(0).isSorcery())) {
                        GuiChoose.one(host + "Revealing cards from library", top);
                    }
                } else if (params.containsKey("RevealValid")) {
                    final String revealValid = params.get("RevealValid");
                    final List<Card> toReveal = CardLists.getValidCards(top, revealValid, host.getController(), host);
                    if (!toReveal.isEmpty()) {
                        GuiChoose.one("Revealing cards from library", toReveal);
                        if (params.containsKey("RememberRevealed")) {
                            for (final Card one : toReveal) {
                                host.addRemembered(one);
                            }
                        }
                    }
                    // Singletons.getModel().getGameAction().revealToCopmuter(top.toArray());
                    // - for when it exists
                } else if (choser.isHuman()) {
                    // show the user the revealed cards
                    GuiChoose.one("Looking at cards from library", top);
                }

                if ((params.containsKey("RememberRevealed")) && !params.containsKey("RevealValid")) {
                    for (final Card one : top) {
                        host.addRemembered(one);
                    }
                }

                if (!noMove) {
                    List<Card> movedCards = new ArrayList<Card>();
                    List<Card> andOrCards = new ArrayList<Card>();
                    for (final Card c : top) {
                        rest.add(c);
                    }
                    if (mitosis) {
                        valid = sharesNameWithCardOnBattlefield(top);
                    } else if (!changeValid.equals("")) {
                        if (changeValid.contains("ChosenType")) {
                            changeValid = changeValid.replace("ChosenType", host.getChosenType());
                        }
                        valid = CardLists.getValidCards(top, changeValid.split(","), host.getController(), host);
                        if (!andOrValid.equals("")) {
                            andOrCards = CardLists.getValidCards(top, andOrValid.split(","), host.getController(), host);
                            andOrCards.removeAll(valid);
                            valid.addAll(andOrCards);
                        }
                        if (valid.isEmpty() && choser.isHuman()) {
                            valid.add(dummy);
                        }
                    } else {
                        valid = top;
                    }

                    if (changeAll) {
                        movedCards.addAll(valid);
                    } else if (params.containsKey("RandomChange")) {
                        int numChanging = Math.min(destZone1ChangeNum, valid.size());
                        movedCards = CardLists.getRandomSubList(valid, numChanging);
                    } else {
                        int j = 0;
                        if (choser.isHuman()) {
                            while ((j < destZone1ChangeNum) || (anyNumber && (j < numToDig))) {
                                // let user get choice
                                if (valid.isEmpty()) {
                                    break;
                                }
                                Card chosen = null;
                                String prompt = "Choose a card to put into the ";
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == -1)) {
                                    prompt = "Chose a card to put on the bottom of the ";
                                }
                                if (destZone1.equals(ZoneType.Library) && (libraryPosition == 0)) {
                                    prompt = "Chose a card to put on top of the ";
                                }
                                if (anyNumber || optional) {
                                    chosen = GuiChoose.oneOrNone(prompt + destZone1, valid);
                                } else {
                                    chosen = GuiChoose.one(prompt + destZone1, valid);
                                }
                                if ((chosen == null) || chosen.getName().equals("[No valid cards]")) {
                                    break;
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = new ArrayList<Card>(andOrCards);
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                                // Singletons.getModel().getGameAction().revealToComputer()
                                // - for when this exists
                                j++;
                            }
                        } // human
                        else { // computer
                            int changeNum = Math.min(destZone1ChangeNum, valid.size());
                            if (anyNumber) {
                                changeNum = valid.size(); // always take all
                            }
                            for (j = 0; j < changeNum; j++) {
                                Card chosen = CardFactoryUtil.getBestAI(valid);
                                if (sa.getActivatingPlayer().isHuman() && p.isHuman()) {
                                    chosen = CardFactoryUtil.getWorstAI(valid);
                                }
                                if (chosen == null) {
                                    break;
                                }
                                if (changeValid.length() > 0) {
                                    GuiChoose.one("Computer picked: ", chosen);
                                }
                                movedCards.add(chosen);
                                valid.remove(chosen);
                                if (!andOrValid.equals("")) {
                                    andOrCards.remove(chosen);
                                    if (!chosen.isValid(andOrValid.split(","), host.getController(), host)) {
                                        valid = andOrCards;
                                    } else if (!chosen.isValid(changeValid.split(","), host.getController(), host)) {
                                        valid.removeAll(andOrCards);
                                    }
                                }
                            }
                        }
                    }
                    if (params.containsKey("ForgetOtherRemembered")) {
                        host.clearRemembered();
                    }
                    Collections.reverse(movedCards);
                    for (Card c : movedCards) {
                        if (c.equals(dummy)) {
                            continue;
                        }
                        final PlayerZone zone = c.getOwner().getZone(destZone1);
                        if (zone.is(ZoneType.Library)) {
                            Singletons.getModel().getGame().getAction().moveToLibrary(c, libraryPosition);
                        } else {
                            c = Singletons.getModel().getGame().getAction().moveTo(zone, c);
                            if (destZone1.equals(ZoneType.Battlefield)) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                                if (params.containsKey("Tapped")) {
                                    c.setTapped(true);
                                }
                            }
                            if (params.containsKey("ExileFaceDown")) {
                                c.setState(CardCharacteristicName.FaceDown);
                            }
                            if (params.containsKey("Imprint")) {
                                host.addImprinted(c);
                            }
                        }
                        if (params.containsKey("ForgetOtherRemembered")) {
                            host.clearRemembered();
                        }
                        if (params.containsKey("RememberChanged")) {
                            host.addRemembered(c);
                        }
                        rest.remove(c);
                    }
                    if (rest.contains(dummy)) {
                        rest.remove(dummy);
                    }

                    // now, move the rest to destZone2
                    if (destZone2.equals(ZoneType.Library)) {
                        if (choser.isHuman()) {
                            // put them in any order
                            while (rest.size() > 0) {
                                Card chosen;
                                if (rest.size() > 1) {
                                    String prompt = "Put the rest on top of the library in any order";
                                    if (libraryPosition2 == -1) {
                                        prompt = "Put the rest on the bottom of the library in any order";
                                    }
                                    chosen = GuiChoose.one(prompt, rest);
                                } else {
                                    chosen = rest.get(0);
                                }
                                Singletons.getModel().getGame().getAction().moveToLibrary(chosen, libraryPosition2);
                                rest.remove(chosen);
                            }
                        } else { // Computer
                            for (int i = 0; i < rest.size(); i++) {
                                Singletons.getModel().getGame().getAction().moveToLibrary(rest.get(i), libraryPosition2);
                            }
                        }
                    } else {
                        // just move them randomly
                        for (int i = 0; i < rest.size(); i++) {
                            Card c = rest.get(i);
                            final PlayerZone toZone = c.getOwner().getZone(destZone2);
                            c = Singletons.getModel().getGame().getAction().moveTo(toZone, c);
                            if (destZone2.equals(ZoneType.Battlefield) && !keywords.isEmpty()) {
                                for (final String kw : keywords) {
                                    c.addExtrinsicKeyword(kw);
                                }
                            }
                        }

                    }
                }
            }
        } // end foreach player
    } // end resolve

    // returns a List<Card> that is a subset of list with cards that share a name
    // with a permanent on the battlefield
    /**
     * <p>
     * sharesNameWithCardOnBattlefield.
     * </p>
     * 
     * @param list
     *            a {@link forge.CardList} object.
     * @return a {@link forge.CardList} object.
     */
    private List<Card> sharesNameWithCardOnBattlefield(final List<Card> list) {
        final List<Card> toReturn = new ArrayList<Card>();
        final List<Card> play = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        for (final Card c : list) {
            for (final Card p : play) {
                if (p.getName().equals(c.getName()) && !toReturn.contains(c)) {
                    toReturn.add(c);
                }
            }
        }
        return toReturn;
    }

    // **********************************************************************
    // ******************************* DigUntil ***************************
    // **********************************************************************


    /**
     * <p>
     * digUntilCanPlayAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
}