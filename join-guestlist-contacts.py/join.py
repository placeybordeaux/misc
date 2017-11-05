import csv
import sys
from functools import reduce

processed_contacts = {}
with open("/home/peter/Documents/contacts.csv", "r", encoding="utf-16") as contacts:
    contact_reader = csv.DictReader(contacts)
    for contact in contact_reader:
        name = contact["Name"]
        email = contact["E-mail 1 - Value"]
        address = contact["Address 1 - Formatted"]
        phone = contact["Phone 1 - Type"]
        processed_contacts[name] = {"email": email, "address": address}

guests = []
with open("/home/peter/Documents/guestlist.csv") as guest_list:
    guest_reader = csv.DictReader(guest_list)
    for guest in guest_reader:
        name = guest["Name"]
        if name in processed_contacts:
            guest.update(processed_contacts[name])
        guests.append(guest)

field_names = reduce(lambda a, b: a.union(b), [set(x.keys()) for x in guests])
with open("/home/peter/Documents/guestlist-joined.csv", "w", encoding="utf-16") as f:
    writer = csv.DictWriter(f, fieldnames=field_names)
    writer.writeheader()
    for row in guests:
        writer.writerow(row)
