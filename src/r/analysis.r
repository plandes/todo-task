proprietary <- "agent,desc,count
buy,Assists in buying goods.,480
service,Do It Yourself type tasks,284
self-improve,Self Improvement/Help,183
school-work,Task related to school,158
contact,Email SMS or call,101
call,Makes a phone call via OS,97
email,Emails a contact via OS,60
calendar,Make an appointment,55
pay-bill-online,Online bill pay,54
find-service,Procure services,42
print,Print out a document,23
postal,Send mail by snail mail,20
plan-meal,Cook or gather ingredients,17
find-travel,Reserve transportation,18
text-sms,Sends SMS text messages,19"

public <- "agent,desc,count
buy-general,Assists in buying general goods,38
buy-grocery,Assists in buying groceries,2
buy-travel,Assists in buying travel,9
buy-wedding,Assists in buying wedding g/s,44
calendar,Make personal appointment,33
contact,Email SMS or phone call,66
household,Schedule time for personal task in calendar,91
how-to,Identify tutorial video,17
office,Schedule office task in calendar,189
office-calendar,Make work appt,8
office-contact,Email SMS or phone call - work,19
pay-bill-online,Online bill pay,18
search-general,General internet search,17
search-recipe,Internet search recipe,4
sell,Sell or donate an item,7
send,Send item by USPS,15"

corp.dist <- function(csvstr, name) {
    df = read.csv(text=csvstr, header=TRUE)
    counts <- df[,3]
    print(sprintf('%s:', name))
    summary(counts)
    print(sprintf('standard deviation: %.2f, varience: %.2f', sd(counts), var(counts)))
}

corp.dist(proprietary, 'proprietary')
corp.dist(public, 'public')
