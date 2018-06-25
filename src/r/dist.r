df <- read.csv('../../results/pruned.csv', header=T)

dist <- data.frame(table(df[,1]))

write.csv(dist, '../../results/dist.csv')
