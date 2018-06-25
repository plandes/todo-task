#install.packages('psych')
#install.packages('irr')
library('psych')
library('irr')

intercoder <- function (df) {
    #df <- df[,][,-1]
    df <- df[,c('annotator1','annotator2')]
    print('cohen:')
    print(cohen.kappa(df))

    print('fleiss:')
    print(kappam.fleiss(df))
}

df <- read.csv('../../results/intercoder-relabeled.csv', header=T)
intercoder(df)
