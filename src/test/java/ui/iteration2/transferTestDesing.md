Предшаги:
1. Пользователь логинится
2. На вкладке "User Dashboard" нажимает Make a Transfer
   Ожидание: открыта вкладка Make a Transfer / New Transfer с
- полями: Select Your Account (-Choose an account-), Recipient Name (пустое), 
Recipient Account Number (пустое), Amount  (пустое)
- кнопками: Send Transfer, New Transfer, Transfer Again, Home, Logout
- чекбоксом Confirm details are correct


**тест1. Пользователь может перевести сумму на свой счет**
предусловия: на счету пользователя сумма не менее 0.01
у пользователя несколько счетов
у пользователя есть name
1) выбрать счет с балансом>0 в Select Your Account
2) в поле Recipient Name ввести свое имя
3) в поле Recipient Account Number ввести номер счета (отличный от счета из шага 1)
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
ожидание: алерт "Successfully transferred $0.01 to account {ACC number из шага 3}!"
баланс счета из шага 1 уменьшился на 0.01 создана transactions с type "TRANSFER_OUT"
баланс счета из шага 3 увеличился на 0.01 создана transactions с type": "TRANSFER_IN"
созданы transactions с type": "TRANSFER_IN", "type": "TRANSFER_OUT"
проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts'

после закрытия алерта открыта страница Make a Transfer
значения полей очищены, кроме Select Your Account, где отображается 
??новый?? баланс счета - нужно уточнение по требованиям


**тест2. Пользователь может перевести сумму на тот же счет, с которого отправляет**
предусловия: на счету пользователя сумма >=10000,00
у пользователя несколько счетов
у пользователя есть name
1) выбрать счет с балансом >=10000,00 в Select Your Account
2) в поле Recipient Name ввести свое имя
3) в поле Recipient Account Number ввести номер того же счета что в Select Your Account
4) в поле Amount ввести сумму 10000
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Successfully transferred $ 10000 to account {ACC number из шагов 1, 3}!"
   баланс счета не изменился, создана transactions с type "TRANSFER_OUT", "TRANSFER_IN"
   проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts'

после закрытия алерта открыта страница Make a Transfer
значения полей очищены, кроме Select Your Account, где отображается
??новый?? баланс счета - нужно уточнение по требованиям

**тест3. Пользователь может перевести сумму на чужой счет (пользователя с таким же именем)**
предусловия: на счету пользователя сумма >=10000,00
у пользователя есть name
у получателя есть счет, name такое же как у отправителя
1) выбрать счет с балансом в Select Your Account
2) в поле Recipient Name ввести имя получателя (такое же как отправителя)
3) в поле Recipient Account Number ввести номер счета получателя
4) в поле Amount ввести сумму 10000
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Successfully transferred $ 10000 to account {ACC number из шага 3}!"
   баланс счета отправителя уменьшился на 10000
   баланс счета получателя увеличился на 10000
   проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts'

после закрытия алерта открыта страница Make a Transfer
значения полей очищены, кроме Select Your Account, где отображается
??новый?? баланс счета - нужно уточнение по требованиям


**тест4. Пользователь может перевести сумму на чужой счет (проверка чуствительности регистра Recipient Name)**
предусловия: на счету пользователя сумма >0
1) выбрать счет с балансом>0 в Select Your Account
2) в поле Recipient Name ввести существующее имя другого пользователя в другом регистре
3) в поле Recipient Account Number ввести номер счета пользователя из шага 2
4) в поле Amount ввести сумму 0,01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Successfully transferred $ 0,01 to account {ACC number из шага 3}!"
   баланс счета отправителя уменьшился на 0,01
   баланс счета получателя увеличился на 0,01
   проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts'

после закрытия алерта открыта страница Make a Transfer
значения полей очищены, кроме Select Your Account, где отображается
??новый?? баланс счета - нужно уточнение по требованиям


**тест5. Пользователь может перевести сумму на чужой счет (проверка пробелов в начале/конце Recipient Name)**
предусловия: на счету пользователя сумма >0
1) выбрать счет с балансом>0 в Select Your Account
2) в поле Recipient Name ввести валидное имя другого пользователя с пробелами в начале / конце
3) в поле Recipient Account Number ввести номер счета пользователя из шага 2
4) в поле Amount ввести сумму 0,01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Successfully transferred $ 0,01 to account {ACC number из шага 3}!"
   баланс счета отправителя уменьшился на 0,01
   баланс счета получателя увеличился на 0,01
   проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts'

после закрытия алерта открыта страница Make a Transfer
значения полей очищены, кроме Select Your Account, где отображается
??новый?? баланс счета - нужно уточнение по требованиям


**тест6. Пользователь может повторить операцию через Transfer Again**
предусловия: у пользователя были транзакции - переводы (10000) и депозиты
на счету пользователя сумма >= 10000,00

предшаги:
клик по Transfer Again
ожидание: открыта вкладка Make a Transfer с
-полем "Search by Username or Name:"
-кнопкой Search Transactions
-блоком Matching Transactions, где отображаются все транзакции пользователя
список соответствует 'GET' \'http://localhost:4111/api/v1/customer/accounts'
с кнопками Repeat напротив каждой транзакции

6.1 Пользователь может повторить ??? перевод TRANSFER_IN (себе же со своего счета) через Transfer Again без редактирования данных
1) клик на Repeat напротив TRANSFER_IN трансфера с чужого счета
ожидание: открыто модальное окно Repeat Transfer с
-Confirm transfer to Account ID: {Account ID} (значение - relatedAccountId из транзакции)
-полем "Select Your Account:" с выпадающим списком счетов и баланса на них
список соответствует 'GET' \'http://localhost:4111/api/v1/customer/accounts'
-полем "Amount:" (заполнено по умолчанию)
-чекбоксом Confirm details are correct (снят по умолчанию)
-кнопками Cancel (активна), Send Transfer (не активна)
2) выбрать счет с достаточым балансом в поле "Select Your Account:" (отличный от счета из Confirm transfer to Account ID)
3) поднять чекбокс Confirm details are correct
ожидание: копка Send Transfer активна
4) нажать Send Transfer
ожидание: алерт "Transfer of $10000 successful from Account {Account ID из шага 2} 
to {Account ID из Confirm transfer to Account ID}!"
после закрытия алерта - открыта вкладка Make a Transfer,
на ней не обновлены Matching Transactions ?? нужно уточнение требований

проверка в 'GET' \'http://localhost:4111/api/v1/customer/accounts':
баланс счета из шага 2 уменьшился на сумму в Amount в шаге 1, появилась транзакция TRANSFER_OUT
баланс счета из Confirm transfer to Account ID: увеличился на сумму в Amount в шаге 1, появилась транзакция TRANSFER_IN


**6.2 Пользователь может повторить перевод (TRANSFER_OUT) через Transfer Again c редактированием данных и
с использованием поиска по Name**
1) в поле "Search by Username or Name:" ввести name пользователя, которому делался перевод (с которым есть TRANSFER_OUT)
2) нажать Search Transactions
ожидание: в Matching Transactions список отфильтрован по Found under:{name} и по username
список соответствует 'GET' \'http://localhost:4111/api/v1/customer/accounts' для relatedAccountId пользователей 
с указанным Name / Username
2) клик на Repeat напротив TRANSFER_OUT трансфера на чужой счет 
   ожидание: открыто модальное окно Repeat Transfer с
   -Confirm transfer to Account ID: {Account ID} (значение - relatedAccountId)
   -полем "Select Your Account:" с выпадающим списком счетов и баланса на них
   -полем "Amount:" (заполнено по умолчанию)
   -чекбоксом Confirm details are correct (не поднят по умолчанию)
   -кнопками Cancel, Send Transfer
2) выбрать счет с достаточым балансом в поле "Select Your Account:"
3) в поле Amount: изменить сумму на другую в пределах баланса счета- с использованием стрелочек в правой части поля
ожидание - клик по стрелке меняет сумму на 1 (увеличение или уменьшение)
3) поднять чекбокс Confirm details are correct
   ожидание: копка Send Transfer активна
4) нажать Send Transfer
   ожидание: алерт "Transfer of ${сумма из шага 3} successful from Account {Account ID счета из шага 2} to 
{Account ID из Confirm transfer to Account ID: }!"
   после закрытия алерта - открыта вкладка Make a Transfer,
   на ней ?? обновлены ?? Matching Transactions - нужно уточнение требований
   баланс счета из шага 2 уменьшился на сумму из шага 3, появилась транзакция TRANSFER_OUT
   баланс счета получателя из Confirm transfer to Account ID увеличился на сумму из шага 3, 
появилась транзакция TRANSFER_IN
   проверка 'GET' \'http://localhost:4111/api/v1/customer/accounts'


6.3 Пользователь может повторить перевод себе же (DEPOSIT) через Transfer Again без редактирования данных
с использованием поиска по username
1) в поле "Search by Username or Name:" ввести username пользователя (свой)
2) нажать Search Transactions
   ожидание: в Matching Transactions список отфильтрован по Found under:{Username} и name
   список соответствует 'GET' \'http://localhost:4111/api/v1/customer/accounts' для "type": "DEPOSIT"
3) клик на Repeat напротив DEPOSIT
   ожидание: открыто модальное окно Repeat Transfer с
   -Confirm transfer to Account ID: {Account ID} (значение - relatedAccountId)
   -полем "Select Your Account:" с выпадающим списком счетов и баланса на них
   список соответствует 'GET' \'http://localhost:4111/api/v1/customer/accounts'
   -полем "Amount:" (заполнено по умолчанию)
   -чекбоксом Confirm details are correct
   -кнопками Cancel (активна), Send Transfer (не активна)
4) выбрать счет с достаточым балансом в поле "Select Your Account:",
   отличный от счета в Confirm transfer to Account ID:
5) поднять чекбокс Confirm details are correct
   ожидание: копка Send Transfer активна
6) нажать Send Transfer
   ожидание: алерт "Transfer of ${Amount} successful from Account {Account ID из шага 2} to {Account ID
   из Confirm transfer to Account ID: }!"
   после закрытия алерта - открыта вкладка Make a Transfer,
   на ней не обновлены Matching Transactions ?? нужно уточнение требований

появились новые транзакции 'GET' \'http://localhost:4111/api/v1/customer/accounts'
TRANSFER_OUT и TRANSFER_IN на сумму которая была указана в Amount в шаге 1
баланс счета из шага 4 уменьшился на сумму Amount в шаге 1
баланс счета из Confirm transfer to Account ID: увеличился на сумму Amount в шаге 1


**тест7. Пользователь не может сделать перевод больше остатка на балансе**
предусловия: на счету пользователя сумма 0.01
1) выбрать счет с балансом 0.01 в Select Your Account
2) в поле Recipient Name ввести валидное имя другого пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага2
4) в поле Amount ввести сумму 0.02
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Error: Invalid transfer: insufficient funds or invalid accounts"
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены


**тест8. Пользователь не может сделать перевод с невалидными данными / отсуствующими данными**

8.1 с невалидным Recipient Name
предусловия: на счету пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя несуществующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета другого пользователя
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "The recipient name does not match the registered name."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.2 с Recipient Account Number, не принадлежащим указанному получателю
предусловия: на счету пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя (не свое)
3) в поле Recipient Account Number номер счета другого пользователя (не из шага 2)
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт с сообщением, что указанный Recipient Account Number у данного Recipient Name отсуствует
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.3 с не существующим Account Number
предусловия: на счету пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести не существующий номер счета
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "No user found with this account number."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.4 с не заполненным полем Select Your Account
предусловия: на счетах пользователя сумма >= 0.01
1) в Select Your Account оставить -Choose an account-
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага 2
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Please fill all fields and confirm."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.5 с не заполненным полем Recipient Name
предусловия: на счетах пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) поле Recipient Name оставить пустым
3) в поле Recipient Account Number ввести валидный номер счета другого пользователя
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Please fill all fields and confirm."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены


8.6 с не заполненным полем Recipient Account Number
предусловия: на счетах пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) поле Recipient Account Number оставить пустым
4) в поле Amount ввести сумму 0.01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Please fill all fields and confirm."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.7 с не заполненным полем Amount
предусловия: на счетах пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага 2
4) поле Amount оставить пустым
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Please fill all fields and confirm."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

8.8 с не поднятым чекбоксом Confirm details are correct
предусловия: на счетах пользователя сумма >= 0.01
1) выбрать счет с положительным балансом в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага 2
4) поле Amount поставить 0,01
4) не поднимать чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Please fill all fields and confirm."
   запрос не отправлен, балансы счетов не изменились

после закрытия алерта открыта страница Make a Transfer
значения полей не очищены

**тест9. Пользователь не может сделать перевод больше допустимого max (10000) и меньше min (0.01)**
в требованиях нет про валидацию Amount на ui - нужно уточнение

10.1 больше максимально допустимого перевода с учетом допустимых значений на ui
предусловия: на счете пользователя сумма > 10000
1) выбрать счет с балансом > 10000 в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага 2
4) поле Amount поставить 10000.0000000000000000001
ожидание: маска числа с 2мя знаками после запятой? тогда лишние знаки отброшены сразу и сумма 10000.00
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Transfer of $10000.00 successful from Account {Account ID из шага 2} to {Account ID
   из Confirm transfer to Account ID: }!"
   после закрытия алерта - открыта вкладка Make a Transfer,
   на ней не обновлены Matching Transactions ?? нужно уточнение требований

появились новые транзакции 'GET' \'http://localhost:4111/api/v1/customer/accounts'
TRANSFER_OUT и TRANSFER_IN на 10000.00
баланс счета из шага 1 уменьшился на 10000.00
баланс счета из Confirm transfer to Account ID: увеличился на 10000.00

10.2 меньше минимально допустимого перевода с учетом допустимых значений на ui
предусловия: на счете пользователя сумма > 0
1) выбрать счет с балансом > 0 в Select Your Account
2) в поле Recipient Name ввести имя существующего пользователя
3) в поле Recipient Account Number ввести валидный номер счета пользователя из шага 2
4) поле Amount поставить 0,0099999999999999999999
   ожидание: маска числа с 2мя знаками после запятой? тогда лишних знаков после запятой нет, сумма 0,01
4) поднять чекбокс Confirm details are correct
5) нажать Send Transfer
   ожидание: алерт "Transfer of $0.01 successful from Account {Account ID из шага 2} to {Account ID
   из Confirm transfer to Account ID: }!"
   после закрытия алерта - открыта вкладка Make a Transfer,
   на ней не обновлены Matching Transactions ?? нужно уточнение требований

появились новые транзакции 'GET' \'http://localhost:4111/api/v1/customer/accounts'
TRANSFER_OUT и TRANSFER_IN на 0.01
баланс счета из шага 1 уменьшился на 0.01
баланс счета из Confirm transfer to Account ID: увеличился на 0.01


**тест10. Пользователь отменяет Repeat Transfer**
1) клик по Transfer Again
2) клик на Repeat напротив TRANSFER_IN  / TRANSFER_OUT /DEPOSIT
   ожидание: открыто модальное окно Repeat Transfer с
   -Confirm transfer to Account ID: {Account ID} (значение - relatedAccountId)
   -полем "Select Your Account:" с выпадающим списком счетов и баланса на них
   -полем "Amount:"
   -чекбоксом Confirm details are correct
   -кнопками Cancel, Send Transfer
2) выбрать счет с достаточым балансом в поле "Select Your Account:"
3) в Amount указать сумму меньше баланса счета из шага 2
4) поднять чекбокс Confirm details are correct
   ожидание: копка Send Transfer активна
4) нажать Cancel
   ожидание: окно закрыто, запрос не отправлен, балансы не изменились, новые транзакции не появились


