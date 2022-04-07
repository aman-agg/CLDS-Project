import java.awt.*;
import java.util.ArrayList;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;



class Entry {
    Point lowerBottom, upperTop;

    public boolean isPoint() {
        if (lowerBottom.x == upperTop.x && upperTop.y == lowerBottom.y) {
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

        SequentialRTree() {
            root = null;
        }

        public void add(Point newPoint) {
            boolean addition = false;
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
                    addition = true;
                    break;
                }
                if (curNode.leftEntry != null && curNode.rightEntry != null) {
                    if (curNode.leftEntry.upperTop != null || curNode.rightEntry.upperTop != null)
                        internal = true;
                    else
                        fullLeaf = true;
                }
                if (internal) {
                    Random random = new Random();
                    int randInt = random.nextInt(2);
                    System.out.println("Traversing while addition to side (0 or 1) : " + randInt);
                    if (randInt % 2 == 0)
                        curNode = curNode.leftChild;
                    else
                        curNode = curNode.rightChild;
                    continue;
                }
                if (fullLeaf) {
                    // add new point and existing left point in a new left child node of cur
                    Node newLeftChild = new Node();
                    newLeftChild.parent = curNode;
                    newLeftChild.leftEntry = new Entry();
                    newLeftChild.leftEntry.lowerBottom = curNode.leftEntry.lowerBottom;
                    newLeftChild.rightEntry = new Entry();
                    newLeftChild.rightEntry.lowerBottom = newPoint;
                    curNode.leftChild = newLeftChild;
                    curNode.leftEntry.upperTop = newPoint;

                    // add existing right point in new right child of cur
                    Node newRightChild = new Node();
                    newRightChild.parent = curNode;
                    newRightChild.leftEntry = new Entry();
                    newRightChild.leftEntry.lowerBottom = curNode.rightEntry.lowerBottom;
                    curNode.rightChild = newRightChild;
                    curNode.rightEntry.upperTop = curNode.rightEntry.lowerBottom;
                    cond = false;
                }
            }
            if (addition) {
                // update MBRs of all ancestors
            }
        }

        public void scan() {
            Queue<Node> q = new LinkedList<>();
            q.add(root);
            q.add(null);
            int level = 0;
            System.out.println("Level "+level);
            while (q.size() != 0) {
                Node curr = q.poll();
                if (curr == null) {
                    if (q.size() == 0) {
                        break;
                    }
                    level++;
                    System.out.println("Level "+level);
                    q.add(null);
                    continue;
                }
                System.out.println("Node: ");
                System.out.print("Left Entry: ");
                printEntry(curr.leftEntry);
                System.out.print("Right Entry: ");
                printEntry(curr.rightEntry);
                if (curr.leftChild != null) {
                    q.add(curr.leftChild);
                }
                if (curr.rightChild != null) {
                    q.add(curr.rightChild);
                }
            }
            System.out.println();
        }

        public void printEntry(Entry e) {
            if (e == null) {
                System.out.print("null");
            } else {
                System.out.print("lowerbottom: " + e.lowerBottom.x + " " + e.lowerBottom.y+" ");
                if (e.upperTop == null) {
                    System.out.println("uppertop null ");
                } else {
                    System.out.println("uppertop: "+ e.upperTop.x +" "+e.upperTop.y+" ");
                }
            }
        }

        public void delete(Point delPoint) {
            Node curr = root;
            while (true) {
                if (curr.leftEntry != null && curr.leftEntry.isPoint() && curr.leftChild == null) {
                    break;
                }
                if (curr.rightEntry != null && curr.rightEntry.isPoint() && curr.rightChild == null) {
                    break;
                }
                if (checkPointInMBR(curr.leftEntry, delPoint)) {
                    curr = curr.leftChild;
                } else {
                    curr = curr.rightChild;
                }
            }
            //Found the node to be deleted
            if (curr.leftEntry != null && curr.rightEntry != null) {
                if (checkPointWithEntry(curr.leftEntry, delPoint)) {
                    curr.leftEntry = null;
                } else if (checkPointWithEntry(curr.rightEntry, delPoint)) {
                    curr.rightEntry = null;
                }
                Node parent = curr.parent;
                if (parent == root) {
                    parent = curr;
                    root = parent;
                    return;
                }
                curr = parent;
            }
            if (curr.leftEntry == null || curr.rightEntry == null) {
                curr.leftEntry = null;
                curr.rightEntry = null;
                if (curr.parent == null) {
                    //curr is root node
                    curr = null;
                    return;
                }
                Node parent = curr.parent;
                if (parent.leftChild.leftEntry == null && parent.leftChild.rightEntry == null) {
                    Node grandParent = parent.parent;
                    if (grandParent == null) {
                        //parent is root
                        parent = parent.rightChild;
                    } else {
                        if (grandParent.leftChild == parent) {
                            grandParent.leftChild = curr;
                            curr.parent = grandParent;
                        } else {
                            grandParent.rightChild = curr;
                            curr.parent = grandParent;
                        }
                    }
                } else {
                    Node grandParent = parent.parent;
                    if (grandParent == null) {
                        //parent is root
                        parent = parent.leftChild;
                    } else {
                        if (grandParent.leftChild == parent) {
                            grandParent.leftChild = curr;
                            curr.parent = grandParent;
                        } else {
                            grandParent.rightChild = curr;
                            curr.parent = grandParent;
                        }
                    }
                }
            }
        }


        public boolean checkPointWithEntry(Entry rect, Point p) {
            if (rect == null) {
                return false;
            }
            if (rect.lowerBottom.x == p.x && rect.lowerBottom.y == p.y) {
                return true;
            }
            return false;
        }

        public boolean checkPointInMBR(Entry rect, Point p) {
            if (rect == null) {
                return false;
            }
            if (p.x < rect.lowerBottom.x || p.x > rect.upperTop.x) {
                return false;
            }
            if (p.y < rect.lowerBottom.y || p.y > rect.upperTop.y) {
                return false;
            }
            return true;
        }
    }
