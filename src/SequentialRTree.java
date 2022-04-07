import java.awt.*;
import java.util.ArrayList;

class Entry{
    Point lowerBottom,upperTop;
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
    public void delete(Point delPoint)
    {

    }
    public ArrayList scan(Point start, Point end)
    {
        return null;
    }
}
