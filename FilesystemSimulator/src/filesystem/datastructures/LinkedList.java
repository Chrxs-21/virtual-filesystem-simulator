/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.datastructures;

/**
 * Lista enlazada simple genérica. Reemplaza a java.util.ArrayList
 * y java.util.LinkedList. Sin uso del Collections Framework.
 * @param <T> Tipo de elementos.
 */
public class LinkedList<T> {
    private Node<T> head;
    private Node<T> tail;
    private int size;

    public LinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    // Métodos se implementarán en el Hito 1
}