/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filesystem.datastructures;

/**
 * Lista enlazada simple genérica implementada manualmente.
 *
 * Reemplaza a java.util.ArrayList y java.util.LinkedList.
 * Usada en todo el proyecto para: cadenas de bloques del disco,
 * hijos de directorios, entradas del journal y solicitudes del planificador.
 *
 * @param <T> Tipo de elementos almacenados.
 */
public class LinkedList<T> {

    /** Primer nodo de la lista. null si está vacía. */
    private Node<T> head;

    /** Último nodo. Permite addLast en O(1) sin recorrer toda la lista. */
    private Node<T> tail;

    /** Cantidad de elementos actuales. */
    private int size;

    /** Crea una lista vacía. */
    public LinkedList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    // ─── INSERCIÓN ──────────────────────────────────────────────────────────

    /**
     * Agrega un elemento al final de la lista. O(1).
     * @param data El dato a agregar.
     */
    public void addLast(T data) {
        Node<T> newNode = new Node<>(data);
        if (tail == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    /**
     * Agrega un elemento al inicio de la lista. O(1).
     * @param data El dato a agregar.
     */
    public void addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.next = head;
            head = newNode;
        }
        size++;
    }

    // ─── ELIMINACIÓN ────────────────────────────────────────────────────────

    /**
     * Elimina y retorna el primer elemento. O(1).
     * @return El dato del nodo eliminado.
     * @throws IllegalStateException si la lista está vacía.
     */
    public T removeFirst() {
        if (head == null) {
            throw new IllegalStateException(
                "No se puede eliminar de una lista vacía."
            );
        }
        T data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    /**
     * Elimina la primera ocurrencia de un elemento. O(n).
     * @param data El dato a eliminar.
     * @return true si fue encontrado y eliminado, false si no existía.
     */
    public boolean remove(T data) {
        if (head == null) return false;

        // Caso especial: el elemento es el head
        if (head.data.equals(data)) {
            removeFirst();
            return true;
        }

        // Buscar el predecesor del nodo a eliminar
        Node<T> current = head;
        while (current.next != null) {
            if (current.next.data.equals(data)) {
                if (current.next == tail) {
                    tail = current;
                }
                current.next = current.next.next;
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    // ─── ACCESO ─────────────────────────────────────────────────────────────

    /**
     * Retorna el elemento en la posición dada sin eliminarlo. O(n).
     * @param index Índice base 0.
     * @return El dato en esa posición.
     * @throws IndexOutOfBoundsException si el índice está fuera de rango.
     */
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(
                "Índice " + index + " fuera de rango. Tamaño: " + size
            );
        }
        Node<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.data;
    }

    /**
     * Retorna el primer elemento sin eliminarlo. O(1).
     * @return El dato del primer nodo.
     * @throws IllegalStateException si la lista está vacía.
     */
    public T getFirst() {
        if (head == null) {
            throw new IllegalStateException("La lista está vacía.");
        }
        return head.data;
    }

    /**
     * Retorna el último elemento sin eliminarlo. O(1).
     * @return El dato del último nodo.
     * @throws IllegalStateException si la lista está vacía.
     */
    public T getLast() {
        if (tail == null) {
            throw new IllegalStateException("La lista está vacía.");
        }
        return tail.data;
    }

    // ─── BÚSQUEDA ───────────────────────────────────────────────────────────

    /**
     * Verifica si un elemento existe en la lista. O(n).
     * @param data El dato a buscar.
     * @return true si existe.
     */
    public boolean contains(T data) {
        Node<T> current = head;
        while (current != null) {
            if (current.data.equals(data)) return true;
            current = current.next;
        }
        return false;
    }

    // ─── UTILIDADES ─────────────────────────────────────────────────────────

    /** @return Cantidad de elementos en la lista. */
    public int size() { return size; }

    /** @return true si la lista no tiene elementos. */
    public boolean isEmpty() { return size == 0; }

    /**
     * Referencia al head para iteración externa nodo a nodo.
     * Usado por la GUI para recorrer sin copiar la lista.
     * @return El primer nodo, o null si la lista está vacía.
     */
    public Node<T> getHead() { return head; }

    /**
     * Elimina todos los elementos de la lista.
     */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Representación en texto para depuración.
     * Formato: [A -> B -> C]
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) sb.append(" -> ");
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}