/*
 * Copyright 2017, Oath Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */
package com.yahoo.elide.core.filter.expression;

import com.yahoo.elide.core.Path;
import com.yahoo.elide.core.Path.PathElement;
import com.yahoo.elide.core.filter.FilterPredicate;
import com.yahoo.elide.core.filter.Operator;

import example.Author;
import example.Book;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests ExpressionScopingVisitor
 */
public class ExpressionScopingVisitorTest {

    private static final String SCIFI = "scifi";
    private static final String GENRE = "genre";
    private static final String NAME = "name";

    @Test
    public void testExpressionCopy() throws Exception {
        Path p1Path = new Path(Arrays.asList(
                new PathElement(Book.class, Author.class, "authors"),
                new PathElement(Author.class, String.class, NAME)
        ));
        FilterPredicate p1 = new FilterPredicate(p1Path, Operator.IN, Arrays.asList("foo", "bar"));

        FilterPredicate p2 = new FilterPredicate(new PathElement(Book.class, String.class, NAME), Operator.IN, Collections.singletonList("blah"));
        FilterPredicate p3 = new FilterPredicate(new PathElement(Book.class, String.class, GENRE), Operator.IN, Collections.singletonList(SCIFI));
        //P4 is a duplicate of P3
        FilterPredicate p4 = new FilterPredicate(new PathElement(Book.class, String.class, GENRE), Operator.IN, Collections.singletonList(SCIFI));

        OrFilterExpression or = new OrFilterExpression(p2, p3);
        AndFilterExpression and1 = new AndFilterExpression(or, p1);
        AndFilterExpression and2 = new AndFilterExpression(and1, p4);
        NotFilterExpression not = new NotFilterExpression(and2);

        PathElement scope = new PathElement(Author.class, String.class, NAME);
        ExpressionScopingVisitor scopingVisitor = new ExpressionScopingVisitor(scope);
        FilterExpression copy = not.accept(scopingVisitor);

        Assert.assertNotEquals(copy, not);

        List<FilterPredicate> predicates = (List) copy.accept(new PredicateExtractionVisitor(new ArrayList<>()));
        List<FilterPredicate> toCompare = Arrays.asList(p2.scopedBy(scope), p3.scopedBy(scope), p1.scopedBy(scope), p4.scopedBy(scope));
        for (int i = 0; i < predicates.size(); i++) {
            FilterPredicate predicateOriginal = toCompare.get(i);
            FilterPredicate predicateCopy = predicates.get(i);
            Assert.assertEquals(predicateCopy, predicateOriginal);
            Assert.assertTrue(predicateCopy != predicateOriginal);
        }
    }
}
