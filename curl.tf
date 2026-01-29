curl -X POST http://localhost:8080/api/transform/VISABA \
  -H "Content-Type: application/json" \
  -H "X-Currency: EUR" \
  -d '{
    "paymentId": "PAY-2026-01-28-002",
    "cdtrNm": "Hans Mueller",
    "cdtrAcctIban": "DE89370400440532013000",
    "cdtrAgtBic": "DEUTDEFF",
    "cdtrAgtNm": "Deutsche Bank",
    "cdtrPstlAdrCtry": "DEU",
    "cdtrPstlAdrTwnNm": "Frankfurt",
    "cdtrPstlAdrStrtNm": "Mainzer Landstrasse 123",
    "dbtrNm": "Euro Corp",
    "dbtrAcctIban": "DE89370400440532013001",
    "dbtrPstlAdrCtry": "DEU",
    "dbtrPstlAdrTwnNm": "Berlin",
    "crPymtAmt": 2500.00,
    "crPymtAmtCcy": "EUR"
  }'
