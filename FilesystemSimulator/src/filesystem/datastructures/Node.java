/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.datastructures;

/**
 * Nodo genérico para la lista enlazada manual.
 * Actúa como el eslabón de una cadena: guarda un dato
 * y apunta al siguiente nodo.
 *
 * @param <T> Tipo de dato almacenado en el nodo.
 */
public class Node<T> {

    /** Dato almacenado en este nodo. */
    public T data;

    /** Referencia al siguiente nodo. null si es el último. */
    public Node<T> next;

    /**
     * Crea un nodo con dato inicial y sin sucesor.
     * @param data El valor a almacenar.
     */
    public Node(T data) {
        this.data = data;
        this.next = null;
    }
}