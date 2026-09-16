import javax.smartcardio.*;
import java.util.List;

public class TestSmartCard {
    public static void main(String[] args) {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            System.out.println("Factory type: " + factory.getType());
            List<CardTerminal> terminals = factory.terminals().list();
            System.out.println("Terminals size: " + terminals.size());
            for (CardTerminal t : terminals) {
                System.out.println("Terminal: " + t.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
