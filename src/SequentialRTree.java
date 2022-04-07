import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

class Entry {
    Point lowerBottom, upperTop;
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

    SequentialRTree() {
        root = null;
    }

    public void add(Point newPoint) {
        boolean addition=false;
        if (newPoint == null)
            return;
        if (root == null) {
            Node newNode = new Node();
            newNode.leftEntry = new Entry();
            newNode.leftEntry.lowerBottom = newPoint;
            root = newNode;
            return;
        }
        Node curNode = root;
        boolean cond = true;
        while (cond) {
            boolean emptyLeaf = false;
            boolean fullLeaf = false;
            boolean internal = false;
            if (curNode == null) {
                cond = false;
                continue;
            }
            if (curNode.leftEntry == null || curNode.rightEntry == null) {
                emptyLeaf = true;
            }
            if (emptyLeaf) {
                if (curNode.leftEntry == null) {
                    curNode.leftEntry = new Entry();
                    curNode.leftEntry.lowerBottom = newPoint;
                } else {
                    curNode.rightEntry = new Entry();
                    curNode.rightEntry.lowerBottom = newPoint;
                }
                addition=true;
                break;
            }
            if (curNode.leftEntry != null && curNode.rightEntry != null) {
                if (curNode.leftEntry.upperTop != null || curNode.rightEntry.upperTop != null)
                    internal = true;
                else
                    fullLeaf = true;
            }
            if (internal) {
                Random random=new Random();
                int randInt = random.nextInt(2);
                System.out.println("Traversing while addition to side (0 or 1) : "+randInt);
                if(randInt%2==0)
                    curNode = curNode.leftChild;
                else
                    curNode = curNode.rightChild;
                continue;
            }
            if (fullLeaf) {
                // add new point and existing left point in a new left child node of cur
                Node newLeftChild = new Node();
                newLeftChild.parent = curNode;
                newLeftChild.leftEntry.upperTop = newPoint;
                newLeftChild.leftEntry.lowerBottom = curNode.leftEntry.lowerBottom;
                curNode.leftChild = newLeftChild;
                curNode.leftEntry.upperTop = newPoint;

                // add existing right point in new right child of cur
                Node newRightChild = new Node();
                newRightChild.parent = curNode;
                newRightChild.leftEntry.lowerBottom = curNode.rightEntry.lowerBottom;
                curNode.rightChild = newRightChild;
                curNode.rightEntry.upperTop = curNode.rightEntry.lowerBottom;
            }
        }
        if(addition)
        {
            // update MBRs of all ancestors
        }
    }

    public void delete(Point delPoint) {

    }

    public ArrayList scan(Point start, Point end) {
        return null;
    }
}