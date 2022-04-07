import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

class Entry{
    Point lowerBottom,upperTop;
    public boolean isPoint(){
        if(lowerBottom.x == upperTop.x && upperTop.y == lowerBottom.y){
            return true;
        }
        return false;
    }
}
class Node {
    Node parent;
    Node leftChild;
    Node rightChild;
    Entry leftEntry;
    Entry rightEntry;
}
public class SequentialRTree {
    Node root;
    SequentialRTree(){
        root=null;
    }
    public void add(Point newPoint)
    {

    }
    public void scan(){
        Queue<Node> q = new LinkedList<>();
        q.add(root);
        q.add(null);
        int level = 0;
        while(q.size() != 0){
            Node curr = q.poll();
            if(curr == null){
                if(q.size() == 0){
                    break;
                }
                System.out.println("Next Level");
                level++;
                q.add(null);
            }
            System.out.println("Node: ");

            printEntry(curr.leftEntry);
            printEntry(curr.rightEntry);
            if(curr.leftChild == null){
                q.add(curr.leftChild);
            }
            if(curr.rightChild == null){
                q.add(curr.rightChild);
            }
        }
    }
    public void printEntry(Entry e){
        if(e == null){
            System.out.print("null");
        }
        else{
            System.out.print("lowerbottom: " + e.lowerBottom.toString()+" ");
            if(e.upperTop == null){
                System.out.println("uppertop null");
            }
            else{
                System.out.println(e.upperTop.toString());
            }
        }
    }
    public void delete(Point delPoint)
    {
        Node curr = root;
        while(true){
            if(curr.leftEntry != null && curr.leftEntry.isPoint() && curr.leftChild == null){
                break;
            }
            if(curr.rightEntry != null && curr.rightEntry.isPoint() && curr.rightChild == null){
                break;
            }
            if(checkPointInMBR(curr.leftEntry,delPoint)){
                curr = curr.leftChild;
            }
            else{
                curr = curr.rightChild;
            }
        }
        //Found the node to be deleted
        if(curr.leftEntry != null && curr.rightEntry != null){
            if(checkPointWithEntry(curr.leftEntry,delPoint)){
                curr.leftEntry = null;
            }
            else if(checkPointWithEntry(curr.rightEntry,delPoint)){
                curr.rightEntry = null;
            }
            Node parent = curr.parent;
            if(parent == root) {
                parent = curr;
                root = parent;
                return;
            }
            curr = parent;
        }
        if(curr.leftEntry == null || curr.rightEntry == null){
            curr.leftEntry = null;
            curr.rightEntry = null;
            if(curr.parent == null){
                //curr is root node
                curr = null;
                return;
            }
            Node parent = curr.parent;
            if(parent.leftChild.leftEntry == null && parent.leftChild.rightEntry == null){
                Node grandParent = parent.parent;
                if(grandParent == null){
                    //parent is root
                    parent = parent.rightChild;
                }
                else{
                    if(grandParent.leftChild == parent){
                        grandParent.leftChild = curr;
                        curr.parent = grandParent;
                    }
                    else{
                        grandParent.rightChild = curr;
                        curr.parent = grandParent;
                    }
                }
            }
            else{
                Node grandParent = parent.parent;
                if(grandParent == null){
                    //parent is root
                    parent = parent.leftChild;
                }
                else{
                    if(grandParent.leftChild == parent){
                        grandParent.leftChild = curr;
                        curr.parent = grandParent;
                    }
                    else{
                        grandParent.rightChild = curr;
                        curr.parent = grandParent;
                    }
                }

            }
            curr = parent;
        }
    }
    public boolean checkPointWithEntry(Entry rect, Point p){
        if(rect == null){
            return false;
        }
        if(rect.lowerBottom.x == p.x && rect.lowerBottom.y == p.y){
            return true;
        }
        return false;
    }
    public boolean checkPointInMBR(Entry rect, Point p){
        if(rect == null){
            return false;
        }
        if(p.x < rect.lowerBottom.x || p.x > rect.upperTop.x){
            return false;
        }
        if(p.y < rect.lowerBottom.y || p.y > rect.upperTop.y){
            return false;
        }
        return true;
    }
    public ArrayList scan(Point start, Point end)
    {
        return null;
    }
}
