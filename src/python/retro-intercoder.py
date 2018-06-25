#!/usr/bin/env python

import csv,pprint, math, sys
import pandas as pd

ann1 = '../../../todocorp/annotated/relabeled.xlsx'
ann2 = '../../../todocorp/annotated/annotator2.xlsx'
relabeled_ic = '../../results/intercoder-relabeled.csv'
pruned_file = '../../results/pruned.csv'

def read_sheet(fname):
    ann = pd.ExcelFile(fname)
    df = ann.parse('todo_corpus')
    dats = []
    for index, row in df.iterrows():
        dats.append(row)
    return dats

def validate(df1, df2):
    if len(df1) != len(df2):
        raise ValueError('length')
    print('lengths validate')
    for i in range(len(df1)):
        r1, r2 = df1[i], df2[i]
        if r1['utterance'] != r2['utterance']:
            raise ValueError('alignment: {}, {}'.format(r1, r2))
    print('utterance validate')

def info(df1, df2):
    print('len: {}, {}'.format(len(df1), len(df2)))

def candidate(val):
    return isinstance(val, str)

def collapse_class(val):
    m = {'buy-general': 'buy',
         'buy-grocery': 'buy',
         'buy-travel': 'buy',
         'buy-wedding': 'buy',
         'search-general': 'search',
         'search-recipe': 'search',
         'office-contact': 'contact',
         'office-calendar': 'calendar',
         'office': 'calendar',
         'household': 'calendar',
    }
    if val in m:
        return m[val]
    return val

def write_intercoder(df1, df2):
    agree = 0
    rows = 0
    with open(relabeled_ic, 'w') as f:
        c_writer = csv.writer(f)
        c_writer.writerow(['annotator1', 'annotator2'])
        for i in range(len(df1)):
            r1, r2 = df1[i], df2[i]
            a1, a2 = r1['class'], r2['class']
            if candidate(a1) and candidate(a2):
                rows = rows + 1
                a2 = collapse_class(a2)
                c_writer.writerow([a1, a2, r1['utterance']])
                #c_writer.writerow([a1, a2])
                if a1 == a2: agree = agree + 1
    print('agree: %.2f (%s/%s)' % ((agree/rows), agree, rows))

def pruned():
    df1 = read_sheet(ann1)
    with open(pruned_file, 'w') as f:
        c_writer = csv.writer(f)
        c_writer.writerow(['class', 'utterance'])
        for r in df1:
            if candidate(r['class']):
                c_writer.writerow([r['class'], r['utterance']])

def main():
    df1 = read_sheet(ann1)
    df2 = read_sheet(ann2)
    validate(df1, df2)
    write_intercoder(df1, df2)
    pruned()
