package sidekick;

import java.util.*;
import javax.swing.tree.DefaultMutableTreeNode;
import org.gjt.sp.jedit.Buffer;
import sidekick.SideKickParser;
import sidekick.SideKickParsedData;
import sidekick.enhanced.SourceAsset;
import javax.swing.text.Position;
public class YAMLParser extends SideKickParser {
    public YAMLParser() {
        super("yaml");
    }

    @Override
    public SideKickParsedData parse(Buffer buffer, errorlist.DefaultErrorSource errorSource) {
        SideKickParsedData data = new SideKickParsedData(buffer.getName());
        DefaultMutableTreeNode root = data.root;
        
        // Track the tree nodes matching each space indentation depth level
        Map<Integer, DefaultMutableTreeNode> levels = new HashMap<>();
        levels.put(-1, root);
        
        int lineCount = buffer.getLineCount();
        for (int i = 0; i < lineCount; i++) {
            String line = buffer.getLineText(i);
            if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            
            // 1. Calculate leading spaces indentation width depth count
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            
            // 2. Parse structural keys (e.g., "my_key:")
            String trimmed = line.trim();
            if (trimmed.contains(":")) {
                String key = trimmed.split(":")[0].trim();
                
                // Calculate absolute character positions for the jEdit document buffer
                final int startOffset = buffer.getLineStartOffset(i) + indent;
                final int endOffset = startOffset + key.length();

                // Construct standard jEdit Position objects 
                Position startPos = buffer.createPosition(startOffset);
                Position endPos = buffer.createPosition(endOffset);
                
                // Construct the Asset directly and use its built-in setters
                Asset yamlAsset = new SourceAsset(key,i+1,startPos);
                //yamlAsset.setStart(startPos);
                //yamlAsset.setEnd(endPos);
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(yamlAsset);
                
                // Find the closest active parent nesting tier higher than the current node
                int parentIndent = indent - 1;
                while (parentIndent >= 0 && !levels.containsKey(parentIndent)) {
                    parentIndent--;
                }
                
                // Fallback cleanly to the core root node if no parent block is discovered
                DefaultMutableTreeNode parentNode = levels.getOrDefault(parentIndent, root);
                parentNode.add(node);
                
                // Map the active position mapping to track subsequent deep lines
                levels.put(indent, node);
                
                // Clear out stale deeper branch histories to prevent crossover contamination
                List<Integer> toRemove = new ArrayList<>();
                for (int k : levels.keySet()) {
                    if (k > indent) toRemove.add(k);
                }
                for (int k : toRemove) levels.remove(k);
            }
        }
        System.out.println("data: "+data);
        return data;
    }
}

