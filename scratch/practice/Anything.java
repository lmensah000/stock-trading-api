package practice;

public class Anything {

     static class Node{

        int data;
        Node next;

        public Node(int data, Node next) {
            this.data = data;
            this.next = next;
        }
        public Node(){

        }

    }

    public static void main(String[] args) {

        Node head = new Node(1, null);
        Node head2 = new Node(2, null);


        head.next = head2;
    }
}
