package forge.gui.home.sanctioned;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiChoose;
import forge.gui.SOverlayUtils;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.CEditorDraftingProcess;
import forge.gui.framework.FScreen;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FOptionPane;
import forge.limited.BoosterDraft;
import forge.limited.LimitedPoolType;
import forge.net.FServer;
import forge.net.Lobby;
import forge.properties.ForgePreferences.FPref;

/** 
 * Controls the draft submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuDraft implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final Command cmdDeckSelect = new Command() {
        @Override
        public void run() {
            VSubmenuDraft.SINGLETON_INSTANCE.getBtnStart().setEnabled(true);
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;

        view.getLstDecks().setSelectCommand(cmdDeckSelect);

        view.getBtnBuildDeck().setCommand(new Command() { @Override
                    public void run() { setupDraft(); } });

        view.getBtnStart().addActionListener(new ActionListener() {
            @Override public void actionPerformed(final ActionEvent e) { startGame(GameType.Draft); } });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        List<Deck> human = new ArrayList<Deck>();
        for (DeckGroup d : Singletons.getModel().getDecks().getDraft()) {
            human.add(d.getHumanDeck());
        }

        final VSubmenuDraft view = VSubmenuDraft.SINGLETON_INSTANCE;
        final JButton btnStart = view.getBtnStart();
        
        view.getLstDecks().setDecks(human);

        if (human.size() > 1) {
            btnStart.setEnabled(true);
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                if (btnStart.isEnabled()) {
                    view.getBtnStart().requestFocusInWindow();
                } else {
                    view.getBtnBuildDeck().requestFocusInWindow();
                }
            }
        });
    }

    private void startGame(final GameType gameType) {
        final boolean gauntlet = !VSubmenuDraft.SINGLETON_INSTANCE.isSingleSelected();
        final Deck humanDeck = VSubmenuDraft.SINGLETON_INSTANCE.getLstDecks().getSelectedDeck();
        final int aiIndex = (int) Math.floor(Math.random() * 7);

        if (humanDeck == null) {
            FOptionPane.showErrorDialog("No deck selected for human.\n(You may need to build a new deck)", "No Deck");
            return;
        } 
        
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.ENFORCE_DECK_LEGALITY)) {
            String errorMessage = gameType.getDecksFormat().getDeckConformanceProblem(humanDeck);
            if (null != errorMessage) {
                FOptionPane.showErrorDialog("Your deck " + errorMessage + " Please edit or choose a different deck.", "Invalid Deck");
                return;
            }
        }

        Singletons.getModel().getGauntletMini().resetGauntletDraft();

        if (gauntlet) {
            int rounds = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName()).getAiDecks().size();
            Singletons.getModel().getGauntletMini().launch(rounds, humanDeck, gameType);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SOverlayUtils.startGameOverlay();
                SOverlayUtils.showOverlay();
            }
        });

 
        DeckGroup opponentDecks = Singletons.getModel().getDecks().getDraft().get(humanDeck.getName());
        Deck aiDeck = opponentDecks.getAiDecks().get(aiIndex);
        if (aiDeck == null) {
            throw new IllegalStateException("Draft: Computer deck is null!");
        }

        List<RegisteredPlayer> starter = new ArrayList<RegisteredPlayer>();
        Lobby lobby = FServer.instance.getLobby();
        starter.add(new RegisteredPlayer(humanDeck).setPlayer(lobby.getGuiPlayer()));
        starter.add(new RegisteredPlayer(aiDeck).setPlayer(lobby.getAiPlayer()));

        Singletons.getControl().startMatch(GameType.Draft, starter);
    }

    /** */
    private void setupDraft() {
        // Determine what kind of booster draft to run
        final String prompt = "Choose Draft Format:";
        final LimitedPoolType o = GuiChoose.oneOrNone(prompt, LimitedPoolType.values());
        if ( o == null ) return;
        
        final CEditorDraftingProcess draft = new CEditorDraftingProcess();
        draft.showGui(new BoosterDraft(o));

        Singletons.getControl().setCurrentScreen(FScreen.DRAFTING_PROCESS);
        CDeckEditorUI.SINGLETON_INSTANCE.setEditorController(draft);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
