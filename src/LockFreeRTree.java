import java.awt.*;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

public class LockFreeRTree implements Runnable {

    //To update the child links in the parent node using CAS
    private static final AtomicReferenceFieldUpdater<Node, Node> leftChildUpdater = newUpdater(Node.class, Node.class, "leftChild");
    private static final AtomicReferenceFieldUpdater<Node, Node> rightChildUpdater = newUpdater(Node.class, Node.class, "rightChild");
    // FOR TESTING PURPOSES
    private static AtomicInteger atomicInteger = new AtomicInteger(0);
    private static AtomicReference<Node> root = new AtomicReference<Node>();
    ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
    // Create the set by newKeySet() method of ConcurrentHashMap
    Set<Integer> set = map.newKeySet();

    LockFreeRTree() {
        root.set(null);
    }

    public static void scan() {
        try {
            if (root.get() == null) {
                System.out.println("Empty tree");
            }
            Queue<Node> q = new LinkedList<>();
            if (root.get() != null) {

                q.add(root.get());

                q.add(null);
                int level = 0;
                System.out.println("Level " + level);
                while (q.size() != 0) {
                    Node curr = q.poll();
                    if (curr == null) {
                        if (q.size() == 0) {
                            break;
                        }
                        level++;
                        System.out.println("Level " + level);
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
            } else {
                System.out.println("Empty tree");
            }
        } finally {

        }
    }

    public static void printEntry(Entry e) {
        if (e == null) {
            System.out.println("null");
        } else {
            System.out.print("lowerbottom: " + e.lowerBottom.x + " " + e.lowerBottom.y + " ");
            if (e.upperTop == null) {
                System.out.println("uppertop null ");
            } else {
                System.out.println("uppertop: " + e.upperTop.x + " " + e.upperTop.y + " ");
            }
        }
    }

    public void add(Point newPoint) {
        if (newPoint == null) {
            System.out.println("Point to be added is not valid");
            return;
        }
        try {
            boolean restartAddition = true;
            while (restartAddition) {
                if (this.contains(newPoint) == true) {
                    System.out.println("Point present in tree");
                    return;
                }

                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Point not present. Trying to add point " + newPoint.toString());

                boolean additionSuccessfull = false;
                if (root.get() == null) {
//                    System.out.println("Case of NULL root (Addition)");
                    Node newNode = new Node();
                    newNode.leftEntry = new Entry();
                    newNode.leftEntry.lowerBottom = new Point(newPoint);
                    if (root.compareAndSet(null, newNode)) {
                        additionSuccessfull = true;
                        restartAddition = false;
                        System.out.println("Thread ID : " + Thread.currentThread().getId() + " Addition Successful - " + newPoint.toString());
                    }
                    continue;
                }
                Node parent = null;
                boolean parentChildLinkLeft = true;
                Node curNode = root.get();
                boolean traversal = true;
                while (traversal) {
                    boolean emptyLeaf = false;
                    boolean fullLeaf = false;
                    boolean internal = false;
                    Node newCurNode = curNode.getCopy();
                    if (curNode == null) {
                        traversal = false;
                        break;
                    }

                    // Case of empty leaf, add a new point
                    if (curNode.leftEntry == null || curNode.rightEntry == null) {
                        emptyLeaf = true;
                        newCurNode = curNode.getCopy();
                        if (newCurNode.leftEntry == null) {
                            newCurNode.leftEntry = new Entry();
                            newCurNode.leftEntry.lowerBottom = new Point(newPoint);
                        } else {
                            newCurNode.rightEntry = new Entry();
                            newCurNode.rightEntry.lowerBottom = new Point(newPoint);
                        }
                    }
                    // check for a internal node or a full leaf
                    else {
                        // distinguish between internal node and a full leaf node using upperTop coordinates or using child links
                        if (curNode.leftEntry.upperTop == null && curNode.rightEntry.upperTop == null)
                            fullLeaf = true;
                        else
                            internal = true;
                    }
                    if (internal) {
                        if (checkAndCompressEmptyInternalNodes(curNode, parent, parentChildLinkLeft)) {
                            traversal = false;
                            break;
                        }
                        int minSide = findMinMBRWhileAdd(newPoint, curNode);
//                    System.out.println("Traversing while addition to side (0 or 1) : " + minSide);
                        if (minSide % 2 == 0) {
                            parent = curNode;
                            curNode = curNode.leftChild;
                            parentChildLinkLeft = true;
                        } else {
                            parent = curNode;
                            curNode = curNode.rightChild;
                            parentChildLinkLeft = false;
                        }
                        continue;
                    }
                    if (fullLeaf) {
                        // add new point and existing nearest point in a new child node of current node
                        long leftDist = calculatePointDistance(newPoint, curNode.leftEntry.lowerBottom);
                        long righDist = calculatePointDistance(newPoint, curNode.rightEntry.lowerBottom);


                        // add existing left/right point and the new point in a left/right child of current node

                        Node newCombinedChild = new Node();
                        newCombinedChild.parent = newCurNode;

                        newCombinedChild.leftEntry = new Entry();
                        newCombinedChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? newCurNode.leftEntry.lowerBottom : newCurNode.rightEntry.lowerBottom);

                        newCombinedChild.rightEntry = new Entry();
                        newCombinedChild.rightEntry.lowerBottom = new Point(newPoint);

                        if (leftDist < righDist) {
                            newCurNode.leftChild = newCombinedChild;
                            newCurNode.leftEntry = calculateMBR(newCombinedChild.leftEntry, newCombinedChild.rightEntry);
                        } else {
                            newCurNode.rightChild = newCombinedChild;
                            newCurNode.rightEntry = calculateMBR(newCombinedChild.leftEntry, newCombinedChild.rightEntry);
                        }
                        // add existing right/left point in new right/left child of current node
                        Node newSingularChild = new Node();
                        newSingularChild.parent = newCurNode;

                        newSingularChild.leftEntry = new Entry();
                        newSingularChild.leftEntry.lowerBottom = new Point(leftDist < righDist ? newCurNode.rightEntry.lowerBottom : newCurNode.leftEntry.lowerBottom);

                        if (leftDist < righDist) {
                            newCurNode.rightChild = newSingularChild;
                            newCurNode.rightEntry = calculateMBR(newSingularChild.leftEntry, newSingularChild.rightEntry);
                        } else {
                            newCurNode.leftChild = newSingularChild;
                            newCurNode.leftEntry = calculateMBR(newSingularChild.leftEntry, newSingularChild.rightEntry);
                        }
                    }
                    // CAS
                    if (emptyLeaf || fullLeaf) {
                        if (parent == null && root.compareAndSet(curNode, newCurNode)) {
                            // CASE of only one leaf node that is root
                            System.out.println("Thread ID : " + Thread.currentThread().getId() + " Addition Successful - " + newPoint.toString());
                            additionSuccessfull = true;
                            restartAddition = false;
                        } else if (parent != null && (parentChildLinkLeft
                                ? leftChildUpdater.compareAndSet(parent, curNode, newCurNode)
                                : rightChildUpdater.compareAndSet(parent, curNode, newCurNode))) {
                            System.out.println("Thread ID : " + Thread.currentThread().getId() + " Addition Successful - " + newPoint.toString());
                            additionSuccessfull = true;
                            restartAddition = false;
                        }
                        traversal = false;
                    }
                }
                if (additionSuccessfull) {
                    // update MBRs of all ancestors
                    updateMBR(curNode);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        } finally {
            System.out.println("Addition Execution completed by " + Thread.currentThread().getId());
        }
    }

    public boolean checkAndCompressEmptyInternalNodes(Node curNode, Node parent, boolean parentChildLinkLeft) {
        if (curNode == null)
            return true;

        Node leftChildOfCurNode = curNode.leftChild;
        Node rightChildOfCurNode = curNode.rightChild;

        // non empty case continue with addition/deletion
        if (leftChildOfCurNode != null || rightChildOfCurNode != null)
            return false;

        // delete the parent's subtree
        if (parent == null)
            root.compareAndSet(curNode, null);
        else if (parentChildLinkLeft
                ? leftChildUpdater.compareAndSet(parent, curNode, null)
                : rightChildUpdater.compareAndSet(parent, curNode, null))
            System.out.println("Thread ID : " + Thread.currentThread().getId() + " Compression successfull when internal node had zero children");
        return true;

    }

    public boolean checkAndCompressSkewedTree(Node curNode, Node parent, boolean parentChildLinkLeft) {
        if (curNode == null)
            return true;

        Node leftChildOfCurNode = curNode.leftChild;
        Node rightChildOfCurNode = curNode.rightChild;

        // non skewed case continue with addition/deletion
        if (leftChildOfCurNode != null && rightChildOfCurNode != null)
            return false;

        if (leftChildOfCurNode == null && rightChildOfCurNode == null) {
            // delete the parent's subtree
            if (parent == null)
                root.compareAndSet(curNode, null);
            else if (parentChildLinkLeft
                    ? leftChildUpdater.compareAndSet(parent, curNode, null)
                    : rightChildUpdater.compareAndSet(parent, curNode, null))
                System.out.println("Thread ID : " + Thread.currentThread().getId() + " Compression successfull");
            return true;
        }

        // curNode now has exactly one child as null. The other can become null afterwards as well
        boolean leftChildNotNull = leftChildOfCurNode != null;

        Node newChildForCurNode = leftChildNotNull ? leftChildOfCurNode.getCopy() : rightChildOfCurNode.getCopy();
        newChildForCurNode.parent = curNode;
        if (leftChildNotNull
                ? leftChildUpdater.compareAndSet(curNode, leftChildOfCurNode, newChildForCurNode)
                : rightChildUpdater.compareAndSet(curNode, rightChildOfCurNode, newChildForCurNode)) {
            // update the parent - curNode link to parent - leftChild link
            Node newParentLinkChild = newChildForCurNode.getCopy();
            if (parent == null) {
                newParentLinkChild.parent = null;
                root.compareAndSet(curNode, newParentLinkChild);
            } else {
                newParentLinkChild.parent = parent;
                if (parentChildLinkLeft
                        ? leftChildUpdater.compareAndSet(parent, curNode, newParentLinkChild)
                        : rightChildUpdater.compareAndSet(parent, curNode, newParentLinkChild))
                    System.out.println("Thread ID : " + Thread.currentThread().getId() + " Compression successfull");
            }
        }
        return true;
    }

    public void updateMBR(Node curNode) {
        // update MBRs of all nodes and its ancestors starting from curNode

        if (curNode == null)
            return;
        boolean isLeftChildEmptyLeaf = false;
        boolean isRightChildEmptyLeaf = false;
        if (curNode.leftChild != null) {
            Entry expectedMBR = calculateMBR(curNode.leftChild.leftEntry, curNode.leftChild.rightEntry);
            if (curNode.leftChild.leftEntry == null || curNode.leftChild.rightEntry == null) {
                isLeftChildEmptyLeaf = true;
            }
            if (!curNode.leftEntry.equals(expectedMBR))
                curNode.leftEntry = expectedMBR;
        }
        if (curNode.rightChild != null) {
            Entry expectedMBR = calculateMBR(curNode.rightChild.leftEntry, curNode.rightChild.rightEntry);
            if (curNode.rightChild.leftEntry == null || curNode.rightChild.rightEntry == null) {
                isRightChildEmptyLeaf = true;
            }
            if (!curNode.rightEntry.equals(expectedMBR))
                curNode.rightEntry = expectedMBR;
        }
        updateMBR(curNode.parent);
    }

    public boolean contains(Point p) {
        try {
            Queue<Node> q = new LinkedList<>();
            if (root.get() == null) {
                return false;
            }
            q.add(root.get());
            q.add(null);
            int level = 0;
            while (q.size() != 0) {
                Node curr = q.poll();
                if (curr == null) {
                    if (q.size() == 0) {
                        break;
                    }
                    level++;
                    q.add(null);
                    continue;
                }
                if (curr.leftEntry != null && curr.leftEntry.isPoint()) {
                    if (p.equals(curr.leftEntry.lowerBottom)) {
                        return true;
                    }
                }
                if (curr.rightEntry != null && curr.rightEntry.isPoint()) {
                    if (p.equals(curr.rightEntry.lowerBottom)) {
                        return true;
                    }
                }
                if (curr.leftChild != null) {
                    q.add(curr.leftChild);
                }
                if (curr.rightChild != null) {
                    q.add(curr.rightChild);
                }
            }
            return false;
        } finally {

        }
    }

    public Entry calculateMBR(Entry leftEntry, Entry rightEntry) {

        Entry ans = new Entry();
        ans.lowerBottom = new Point();
        if (leftEntry != null) {
            ans.lowerBottom.x = (int) leftEntry.lowerBottom.getX();
            ans.lowerBottom.y = (int) leftEntry.lowerBottom.getY();
        }
        if (rightEntry != null) {
            ans.lowerBottom.x = (int) Math.min(rightEntry.lowerBottom.getX(), ans.lowerBottom.x);
            ans.lowerBottom.y = (int) Math.min(rightEntry.lowerBottom.getY(), ans.lowerBottom.y);
        }

        ans.upperTop = new Point();
        if (leftEntry != null) {
            ans.upperTop.x = (int) (leftEntry.upperTop != null ? leftEntry.upperTop.getX() : leftEntry.lowerBottom.getX());
            ans.upperTop.y = (int) (leftEntry.upperTop != null ? leftEntry.upperTop.getY() : leftEntry.lowerBottom.getY());
        }
        if (rightEntry != null) {
            ans.upperTop.x = (int) Math.max(ans.upperTop.getX(), rightEntry.upperTop != null ? rightEntry.upperTop.getX() : rightEntry.lowerBottom.getX());
            ans.upperTop.y = (int) Math.max(ans.upperTop.getY(), rightEntry.upperTop != null ? rightEntry.upperTop.getY() : rightEntry.lowerBottom.getY());
        }

        return ans;
    }

    public int findMinMBRWhileAdd(Point newPoint, Node curNode) {
        // return 0 for left child and 1 for right child

        // check if new point lies in left MBR

        if (curNode.leftEntry != null) {
            if (pointInRectOrNot(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop))
                return 0;
        }
        if (curNode.rightEntry != null) {
            if (pointInRectOrNot(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop))
                return 1;
        }

        long leftMBRArea = calculateNewRectArea(newPoint, curNode.leftEntry.lowerBottom, curNode.leftEntry.upperTop);
        long rightMBRArea = calculateNewRectArea(newPoint, curNode.rightEntry.lowerBottom, curNode.rightEntry.upperTop);

        return leftMBRArea <= rightMBRArea ? 0 : 1; //

    }

    public long calculateNewRectArea(Point newPoint, Point rectLowerBottom, Point rectUpperTop) {
        double minX = Math.min(newPoint.getX(), rectLowerBottom.getX());
        double maxX = Math.max(newPoint.getX(), rectUpperTop.getX());
        double minY = Math.min(newPoint.getY(), rectLowerBottom.getY());
        double maxY = Math.max(newPoint.getY(), rectUpperTop.getY());
        return (long) ((maxX - minX) * (maxY - minY));
    }

    public boolean pointInRectOrNot(Point point, Point rectLowerBottom, Point rectUpperTop) {
        if (point.getX() >= rectLowerBottom.getX() &&
                point.getX() <= rectUpperTop.getX() &&
                point.getY() >= rectLowerBottom.getY() &&
                point.getY() <= rectUpperTop.getY()
        )
            return true;
        return false;
    }

    public long calculatePointDistance(Point a, Point b) {
        return (long) Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    public void delete(Point delPoint) {
        try {
            boolean restartDeletion = true;
            while (restartDeletion) {
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Point present. Trying to del point " + delPoint.toString());
                Node curr = root.get();
//                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion restarted " + delPoint.toString());

                if (delPoint == null) {
                    return;
                }
                Node newNode = null;
                Node parent = null;

                boolean compression_false = false;
                boolean emptyLeaf = false;
                boolean fullLeaf = false;
                boolean internal = false;
                boolean isParentChildLinkLeft = true;  //null if current node is root
                boolean foundPoint = false;
                Queue<Node> q = new LinkedList<>();
                Queue<Boolean> parentLinks = new LinkedList<>();
                if (root.get() == null) {
                    System.out.println("Root is null, cannot delete");
                    return;
                }
                q.add(root.get());
                parentLinks.add(false);
                while (q.size() != 0) {
                    emptyLeaf = false;
                    fullLeaf = false;
                    internal = false;
                    curr = q.poll();
                    Node leftChildOfCurNode = curr.leftChild;
                    Node rightChildOfCurNode = curr.rightChild;
                    parent = curr.parent;
                    isParentChildLinkLeft = parentLinks.poll();
                    if (curr == null) {
//                        System.out.println(foundPoint);
//                        curr.printNode();
                        break;
                    }
                    if (curr.leftEntry == null || curr.rightEntry == null) {
                        emptyLeaf = true;
                    }
                    // check for a internal node or a full leaf
                    else {
                        // distinguish between internal node and a full leaf node using upperTop coordinates or using child links
                        if (curr.leftEntry.upperTop == null && curr.rightEntry.upperTop == null && curr.leftChild == null && curr.rightChild == null)
                            fullLeaf = true;
                        else
                            internal = true;
                    }
                    if (emptyLeaf || fullLeaf) {
                        if (curr.leftEntry != null && delPoint.equals(curr.leftEntry.lowerBottom)) {
                            //Point found
                            foundPoint = true;
                            System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: found point " + delPoint.toString());
                            break;
                        }
                        if (curr.rightEntry != null && delPoint.equals(curr.rightEntry.lowerBottom)) {
                            //Point found
                            System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: found point " + delPoint.toString());
                            foundPoint = true;
                            break;
                        }
                    } else {
//                        if (checkAndCompressSkewedTree(curr, parent, isParentChildLinkLeft)) {
//                            compression_false = true;
//                            break;
//                        }
//                         Curr node is Internal Node
                        if (leftChildOfCurNode != null) {
                            parentLinks.add(true);
                            q.add(leftChildOfCurNode);
                        }
                        if (rightChildOfCurNode != null) {
                            parentLinks.add(false);
                            q.add(rightChildOfCurNode);
                        }
                    }
                }
                if (compression_false)
                    continue;
                if (!foundPoint) {
                    System.out.println("Thread ID : " + Thread.currentThread().getId() + " FOUND POINT FALSE ");
                    break;
                }

//                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Traversal completed ");
                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Traversal ended " + isParentChildLinkLeft);
                //This if is to create the newNode for replacement
                if (fullLeaf) {
                    //full leaf case
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Full leaf case ");
                    newNode = curr.getCopy();
                    if (curr.leftEntry == null || curr.rightEntry == null) {
                        continue;
                    }
                    if (curr.leftEntry.lowerBottom.equals(delPoint)) {
                        newNode.leftEntry = null;
                    } else if (curr.rightEntry.lowerBottom.equals(delPoint)) {
                        newNode.rightEntry = null;
                    }
                } else if (emptyLeaf) {
                    //Empty Leaf case
                    if (curr.leftEntry == null && curr.rightEntry == null) {
                        continue;
                    }
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: empty leaf case ");
                    newNode = null;
                }
//                System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Point present. Found leaf case " + fullLeaf);
                if (parent == null && root.compareAndSet(curr, newNode)) {
                    //current node is root node
                    System.out.println("Thread Id : " + Thread.currentThread().getId() + " Deletion: Parent null cas ");
                    restartDeletion = false;
                    return;
                } else if (parent != null && isParentChildLinkLeft
                        ? leftChildUpdater.compareAndSet(parent, curr, newNode)
                        : rightChildUpdater.compareAndSet(parent, curr, newNode)) {
                    System.out.println("Thread ID : " + Thread.currentThread().getId() + " Deletion: Parent not null CAS");
                    restartDeletion = false;
                    return;
                }
//                    if (!restartDeletion) {
//                        //updateMBR(newNode);
//                    }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Deletion Execution completed by " + Thread.currentThread().getId());
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

    @Override
    public void run() {
        //Different test cases
//        int[] operations = {1,1,1,2,1,2,1,2};
//        int[][] inputs = {{1,1},{2,2},{3,3},{2,2},{4,4},{2,2},{5,5},{3,3}};
        int[] operations = {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 6}; // for full leaf non root deletion
        int[][] inputs = {{1, 1}, {3, 3}, {1, 2}, {2, 3}, {3, 1}, {2, 1}, {3, 3}, {1, 2}, {2, 3}, {3, 1}, {2, 1}, {1,1} };
//        int[] operations = {1,1,1,2,2,2,1};
//        int[][] inputs = {{1,1},{2,2},{1,1},{2,2},{1,1},{2,2},{1,4}};
//        int[] operations = {1,1,1,1,1,2,1};
//        int[][] inputs = {{1,1},{2,2},{3,3},{1,1},{2,4},{2,2},{1,4}};
//        int[] operations = {1,1,2,2,2,2,1};
//        int[][] inputs = {{1,1},{2,2},{2,2},{2,2},{1,1},{2,2},{1,4}};
        Scanner sc = new Scanner(System.in);
        int prevAtomicValue = atomicInteger.get();
        int newAtomicValue = prevAtomicValue + 1;
        while (true) {
            if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue))
                break;
            prevAtomicValue = atomicInteger.get();
            newAtomicValue = prevAtomicValue + 1;
        }
        int opInd = newAtomicValue;
        System.out.println("Operation Index chosen by Thread " + Thread.currentThread().getId() + " : " + opInd);
//        while(threadSafeUniqueNumbers.contains(opInd) == false){
//            opInd = rand.nextInt(operations.length);
//        }
        if (set.contains(opInd)) {
            System.out.println("Thread ID : " + Thread.currentThread().getId() + " Already in set");
        } else {
            set.add(opInd);
            int operation = operations[opInd];
            if (operation == 1) {
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x, y);
//                System.out.println("Thread ID : " + Thread.currentThread().getId() + " Stage 1 Addition: " + temp.toString());
                this.add(temp);
//                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Addition");
                //this.scan();
                int prev = atomicInteger.get();
                while (!atomicInteger.compareAndSet(prev, prev + 1)) {
                    prev = atomicInteger.get();
                }
//                System.out.println("Thread ID : " + Thread.currentThread().getId() + " Atomic counter " + atomicInteger);
//                if(atomicInteger.get()==8) {
//                    while (true) {
//                        prevAtomicValue = atomicInteger.get();
//                        newAtomicValue = prevAtomicValue + 1;
//                        if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue)) {
////                            this.delete(new Point(2,2));
////                            this.scan();
////                            this.delete(new Point(3,3));
//                            this.scan();
//                            break;
//                        }
//                    }
//                }
                while (true) {
                    if (atomicInteger.get() == 16) {
                        prevAtomicValue = 16;
                        newAtomicValue = prevAtomicValue + 1;
                        if (atomicInteger.compareAndSet(prevAtomicValue, newAtomicValue)) {
                            this.scan();
                            System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Scan in Addition");
                            atomicInteger.compareAndSet(newAtomicValue, newAtomicValue + 1);
//                            this.add(new Point(4,5));
                            break;
                        }
                    } else if (atomicInteger.get() >= 18)
                        break;
                }

            } else if (operation == 2) {
//                System.out.print("Deletion: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x, y);
//                System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Stage 1 Deletion: "+temp.toString());
                //this.delete(temp);
//                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Deletion");
//                this.scan();
//                int prev = atomicInteger.get();
//                while (!atomicInteger.compareAndSet(prev, prev + 1)) {
//                    prev = atomicInteger.get();
//                }
//                System.out.println("Thread ID : " + Thread.currentThread().getId() + " Atomic counter " + atomicInteger);
                while (true) {
                    int curatomicvalue = atomicInteger.get();
                    if (curatomicvalue >= 18) {
                        newAtomicValue = curatomicvalue + 1;
                        if (atomicInteger.compareAndSet(curatomicvalue, newAtomicValue)) {
//                            this.scan();
                            this.delete(temp);
//                            if(atomicInteger.get()==30)
                            System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed delete after additions and 1 scan");
                            atomicInteger.compareAndSet(atomicInteger.get(), atomicInteger.get() + 1);
                            if (atomicInteger.get() == 28) {
                                this.scan();
                                atomicInteger.compareAndSet(28,29);
                            }
                            break;
                        }
                    }
                }

            } else if (operation == 3) {
                System.out.println("Scan");
                this.scan();
                System.out.println(Thread.currentThread().getId());
            } else if (operation == 4) {
                System.out.print("Contains: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                System.out.println();
                System.out.println(this.contains(new Point(x, y)));
                System.out.println(Thread.currentThread().getId());
            } else if (operation == 5) {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
            } else if (operation == 6) {
//                System.out.print("Deletion: ");
                int x = inputs[opInd][0];
                int y = inputs[opInd][1];
                Point temp = new Point(x, y);
//                System.out.println("Thread ID : "+ Thread.currentThread().getId()+ " Stage 1 Deletion: "+temp.toString());
                //this.delete(temp);
//                System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed Deletion");
//                this.scan();
//                int prev = atomicInteger.get();
//                while (!atomicInteger.compareAndSet(prev, prev + 1)) {
//                    prev = atomicInteger.get();
//                }
//                System.out.println("Thread ID : " + Thread.currentThread().getId() + " Atomic counter " + atomicInteger);
                while (true) {
                    int curatomicvalue = atomicInteger.get();
                    if (curatomicvalue == 29) {
                        newAtomicValue = curatomicvalue + 1;
                        if (atomicInteger.compareAndSet(curatomicvalue, newAtomicValue)) {
//                            this.scan();
                            this.add(temp);
//                            if(atomicInteger.get()==30)
                            System.out.println("Thread ID: " + Thread.currentThread().getId() + " Completed add after 5 additions and 5 deletions");
                            this.scan();
                            break;
                        }
                    }
                }

            }else {
                System.out.println("Thread ID: " + Thread.currentThread().getId());
                System.out.println("Incorrect input");
            }
        }
    }
}
