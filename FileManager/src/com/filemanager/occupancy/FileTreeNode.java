package com.filemanager.occupancy;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by wuhao on 2015/6/15.
 */
public class FileTreeNode<T> implements Iterable<T> {

    //data use for file
    public T data;
    //the file path
    public long size;
    FileTreeNode<T> parent = null;
    LinkedList<FileTreeNode<T>> children;

    public FileTreeNode(T data) {
        this.data = data;
        this.children = new LinkedList<FileTreeNode<T>>();
    }

    public FileTreeNode<T> addChild(T child) {
        FileTreeNode<T> childNode = new FileTreeNode<T>(child);
        childNode.parent = this;
        this.children.add(childNode);
        return childNode;
    }

    @Override
    public Iterator<T> iterator() {
        //add something
        return null;
    }
}

