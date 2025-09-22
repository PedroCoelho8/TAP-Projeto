[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/JQuLlSSl)

* The intention of this part of the project is to change the create method:
* - create a way to have multiple tasks being done at the same time.
* - the same resource cannot be used at the same time by two tasks.
* - the complete schedule must schedule all the tasks of all the products needed
*
* Based on this requirements there are a few things that can be done:
* - remain with the same beginning of ProductionParser.parseProduction(xml)
* - change completely the way the time is being held
* - multiple tasks may start at the same time or during another tasks' creation
*
* For example, consider this:
* - Order1(Product1); Order2(Product2)
* - Product1(Task1, Task2); Product2(Task1);
* - Task1(100, Physical1); Task2(120, Physical2);
* - Human1(Physical1); Human2(Physical2)
*
* In this case the result would be 3 TaskSchedules:
* - TaskSchedule1(0, 100, Order1, Task1)
* - TaskSchedule2(100, 200, Order2, Task1)
* - TaskSchedule3(100, 220, Order1, Task2)
*
* QUESTIONS
*
* Should there be a defined order? For example, in the case above, TaskSchedule3 was defined as the 3rd because it's the one with the biggest time, but the result could've been:
*
* - TaskSchedule1(0, 100, Order1, Task1)
* - TaskSchedule2(100, 220, Order1, Task2)
* - TaskSchedule3(100, 200, Order2, Task1)
*
* The first one is by time, this one is be Order's number
* TAP MILESTONE 3

-Nos temos de gerar ficheiros de saída;

-Temos de criar ficheiros certos à mão? Sim, mas somos nos que os fazemos a la pata

-Temos de colocar no fim o maior “endtime”?  Não temos que nos preocupar.

-Nao podemos paralelizar as tarefas de um mesmo produto

- Um humano só pode usar um recurso físico
- Um recurso físico só pode ser utilizada ao mesmo tempo uma vez
- Deve haver uma consolidação do melhor tempo: ler o tempo total de cada task se for necessário começar 2 ao mesmo tempo
- Vamos supor que o Joao pode usar o fisico 1
- A Maria pode usar o fisico 2, 3
- A tarefa 1 usa o fisico 1,2
- A tarefa 2 usa o fisico 1,3
- Ha 2 produtos, 1 com cada tarefa
- A ordem poderia ser 
- Antonio -> 1,1 
- Maria -> *Esperar o 1*, 2 ,*Esperar o 1*, 3
- Mas qual é o problema?
- Se o 2 durar 2 segundos e o 3 durar 3 segundos, esta execução durará 13 segundos, quando poderia ser possível, considerando estas durações
- Maria: *Esperar o 1*, 3 ,*Esperar o 1*, 2
- Que duraria 12 segundos
- Temos de considerar que o tempo total de um produto deve ser comparado em cada iteração

