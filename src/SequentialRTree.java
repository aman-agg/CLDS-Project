import java.awt.*;
import java.util.ArrayList;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;



class Entry {
    Point lowerBottom, upperTop;
    Entry()
    {
        lowerBottom=null;
        upperTop=null;
    }
    public boolean isPoint() {
        if (upperTop ==null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Entry) {
            Entry object = (Entry) obj;
            if (object.upperTop.equals(this.upperTop) && object.lowerBottom.equals(this.lowerBottom))
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
            boolean additionSuccessfull = false;
            if (newPoint == null)
                return;
            if (root == null) {
                Node newNode = new Node();
                newNode.leftEntry = new Entry();
                newNode.leftEntry.lowerBottom = new Point(newPoint);
                root = newNode;
                additionSuccessfull= true;
                System.out.println("Added new point - "+ newPoint.getX()+" "+newPoint.getY());
                return;
            }
            Node curNode = root;
            boolean traversal = true;
            while (traversal) {
                boolean emptyLeaf = false;
                boolean fullLeaf = false;
                boolean internal = false;
                if (curNode == null) {
                    traversal = false;
                    continue;
                }
                if (curNode.leftEntry == null || curNode.rightEntry == null) {
                    emptyLeaf = true;
                }
                // In case of empty leaf, add a new point
                if (emptyLeaf) {
                    if (curNode.leftEntry == null) {
                        curNode.leftEntry = new Entry();
                        curNode.leftEntry.lowerBottom = new Point(newPoint);
                    } else {
                        curNode.rightEntry = new Entry();
                        curNode.rightEntry.lowerBottom = new Point(newPoint);
                    }
                    additionSuccessfull = true;
                    break;
                }
                // check for a internal node or a full leaf
                if (curNode.leftEntry != null && curNode.rightEntry != null) {
                    // distinguish between internal node and a full leaf node using upperTop coordinates or using child links
                    if (curNode.leftEntry.upperTop == null && curNode.rightEntry.upperTop == null)
                        fullLeaf = true;
                    else
                        internal=true;
                }
                if (internal) {
                    int minSide = findMinMBRWhileAdd(newPoint, curNode);
                    System.out.println("Traversing while addition to side (0 or 1) : " + minSide);
                    if (minSide % 2 == 0)
                        curNode = curNode.leftChild;
                    else
                        curNode = curNode.rightChild;
                    continue;
                }
                if (fullLeaf) {
                    // add new point and existing nearest point in a new child node of current node
                    long leftDist = calculatePointDistance(newPoint, curNode.leftEntry.lowerBottom);
                    long righDist = calculatePointDistance(newPoint, curNode.rightEntry.lowerBottom);

                    // add existing left/right point and the new point in a left/right child of current node
                    Node newCombinedChild = new Node();
                    newCombinedChild.parent = curNode;

                    newCombinedChild.leftEntry = new Entry();
                    newCombinedChild.leftEntry.lowerBottom = new Point(leftDist<righDist ? curNode.leftEntry.lowerBottom : curNode.rightEntry.lowerBottom);

                    newCombinedChild.rightEntry = new Entry();
                    newCombinedChild.rightEntry.lowerBottom = new Point(newPoint);

                    if(leftDist<righDist)
                        curNode.leftChild = newCombinedChild;
                    else
                        curNode.rightChild = newCombinedChild;

                    // add existing right/left point in new right/left child of current node
                    Node newSingularChild = new Node();
                    newSingularChild.parent = curNode;

                    newSingularChild.leftEntry = new Entry();
                    newSingularChild.leftEntry.lowerBottom = new Point(leftDist<righDist ? curNode.rightEntry.lowerBottom : curNode.leftEntry.lowerBottom);

                    if(leftDist<righDist)
                        curNode.rightChild = newSingularChild;
                    else
                        curNode.leftChild = newSingularChild;

                    additionSuccessfull=true;
                    traversal = false;
                }
            }

            if (additionSuccessfull) {
                System.out.println("Added new point - "+ newPoint.getX()+" "+newPoint.getY());
                // update MBRs of all ancestors
                updateMBR(curNode);
            }

        }
        public void updateMBR(Node curNode)
        {
            // update MBRs of all nodes and its ancestors starting from curNode
            if(curNode==null)
                return;
            if(curNode.leftChild!=null)
            {
                Entry expectedMBR = calculateMBR(curNode.leftChild.leftEntry, curNode.leftChild.rightEntry);
                if(!curNode.leftEntry.equals(expectedMBR))
                    curNode.leftEntry = expectedMBR;
            }
            if(curNode.rightChild!=null)
            {
                Entry expectedMBR = calculateMBR(curNode.rightChild.leftEntry, curNode.rightChild.rightEntry);
                if(!curNode.rightEntry.equals(expectedMBR))
                    curNode.rightEntry = expectedMBR;
            }
            updateMBR(curNode.parent);
        }
        public Entry calculateMBR(Entry leftEntry, Entry rightEntry)
        {
            Entry ans = new Entry();
            ans.lowerBottom = new Point();
            if(leftEntry!=null) {
                ans.lowerBottom.x = (int) leftEntry.lowerBottom.getX();
                ans.lowerBottom.y = (int) leftEntry.lowerBottom.getY();
            }
            if(rightEntry!=null)
            {
                ans.lowerBottom.x = (int)Math.min(rightEntry.lowerBottom.getX(), ans.lowerBottom.x);
                ans.lowerBottom.y = (int)Math.min(rightEntry.lowerBottom.getY(), ans.lowerBottom.y);
            }

            ans.upperTop = new Point();
            if(leftEntry!=null)
            {
                ans.upperTop.x = (int)(leftEntry.upperTop!=null ? leftEntry.upperTop.getX() : leftEntry.lowerBottom.getX());
                ans.upperTop.y = (int)(leftEntry.upperTop!=null ? leftEntry.upperTop.getY() : leftEntry.lowerBottom.getY());
            }
            if(rightEntry!=null)
            {
                ans.upperTop.x = (int) Math.max(ans.upperTop.getX(), rightEntry.upperTop!=null ? rightEntry.upperTop.getX() : rightEntry.lowerBottom.getX());
                ans.upperTop.y = (int) Math.max(ans.upperTop.getY(), rightEntry.upperTop!=null ? rightEntry.upperTop.getY() : rightEntry.lowerBottom.getY());
            }

            return ans;
        }
        public int findMinMBRWhileAdd(Point newPoint, Node curNode)
        {
            // return 0 for left child and 1 for right child

            // check if new point lies in left MBR
            if(curNode.leftEntry!=null){
                if(pointInRectOrNot(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop))
                    return 0;
            }
            if(curNode.rightEntry!=null)
            {
                if(pointInRectOrNot(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop))
                    return 1;
            }

            long leftMBRArea = calculateNewRectArea(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop);
            long rightMBRArea = calculateNewRectArea(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop);

            return leftMBRArea<=rightMBRArea ? 0 : 1; //
        }
        public long calculateNewRectArea(Point newPoint, Point rectLowerBottom, Point rectUpperTop)
        {
            double minX = Math.min(newPoint.getX(), rectLowerBottom.getX());
            double maxX = Math.max(newPoint.getX(), rectUpperTop.getX());
            double minY = Math.min(newPoint.getY(), rectLowerBottom.getY());
            double maxY = Math.max(newPoint.getY(), rectUpperTop.getY());
            return (long)((maxX-minX)*(maxY-minY));
        }
        public boolean pointInRectOrNot(Point point, Point rectLowerBottom, Point rectUpperTop)
        {
            if( point.getX()>=rectLowerBottom.getX() &&
                    point.getX()<=rectUpperTop.getX() &&
                    point.getY()>=rectLowerBottom.getY() &&
                    point.getY()<=rectUpperTop.getY()
            )
                return true;
            return false;
        }
        public long calculatePointDistance(Point a, Point b)
        {
            return (long)Math.sqrt(Math.pow(a.x-b.x,2)+Math.pow(a.y-b.y,2));
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
            System.out.println("Leaf node which contains the delPoint----------");
            System.out.print("Left Entry: ");
            printEntry(curr.leftEntry);
            System.out.print("Right Entry: ");
            printEntry(curr.rightEntry);
            System.out.println("------------");
            //Found the node to be deleted
            boolean fullLeaf = false;
            boolean emptyLeaf = false;
            if(curr.leftEntry != null && curr.rightEntry != null){
                fullLeaf = true;
            }
            else{
                emptyLeaf = true;
            }
            if (fullLeaf) {
                if (checkPointWithEntry(curr.leftEntry, delPoint)) {
                    curr.leftEntry = null;
                } else if (checkPointWithEntry(curr.rightEntry, delPoint)) {
                    curr.rightEntry = null;
                }
                Node parent = curr.parent;
                if(parent == null){
                    //curr is the root node
                    root = curr;
                    return;
                }
                else if(parent == root){
                    root = parent;
                    curr = parent;
                    updateMBR(curr);
                    return;
                }
                else {
                    //parent is the root node
                    curr = parent;
                    updateMBR(curr);
                    return;
                }

            }
            if (emptyLeaf) {
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
                        parent.parent =null;
                        root = parent;
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
                        parent.parent =null;
                        root = parent;
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
                curr = parent;
                updateMBR(curr);
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
