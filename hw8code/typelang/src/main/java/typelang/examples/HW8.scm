//Q6

//:Q1a
(deref (ref: num 45)) //Test valid type
(deref 45) //Test invalid type

//:Q1b
(set! (ref: num 0) 1) //Test valid type
(set! (ref: num 0) #t) //Test invalid type in

//:Q2a
(car (cons 1 2)) //Test valid type
(car 2) //Test invalid type

//:Q2b
(list : num 1 2 3 4 5) //Test valid types
(list : num 1 2 3 4 #f) //Test for any invalid type in list

//:Q3
(define add : (num num num -> num) (lambda (x: num y: num z: num) (+x (+y z)))) //Valid
(add 5 6 7) //Valid, shows usage of defined lambda
(* 2 5) //Valid, shows type polymorphism functional
(add 5 56 #t) //Invalid, some arg is not num
(3 4) //Invalid, handled by CallExp by default, not FuncT in first arg

//:Q4
(< 3 4) //valid usage
(< 3 #f) //Invalid usage, not NumT

//:Q5
(if (> 1 10) (list: num 1 1 2 3) (list: num 1 2 3 5)) //Valid
(if (< 1 10) (list: num 1 2 3) 42) //Invalid, then and else are not same type
(if 1 2 3) //Invalid, cond is not bool

//Q7 : These are my attempts. I can't seem to get them working, with or without recursion.
(define processlists: (lam list list -> list)
    (lambda (op: lam list1: list list2: list)
        (if (null? list1)
            (list)
            (if (null? list2)
                (list)
                (cons (op (car list1) (car list2)) (processlists (cdr list1) (cdr list2)))
            )
        )
    )
)


(define common: (list list -> list)
    (lambda (pair1: list pair2: list)
        (if (= (car pair1) (car pair2))
            (if (= (car (cdr pair1)) (car (cdr pair2)))
                pair1
                (cons -1 -1)
            )
            (cons -1 -1)
        )
    )
)

(define padd : (lam num num num -> num) (lambda (p: lam x: num y: num z: num) (+ x (+y z))))

(define blah: (list -> num) (lambda (p: list) (car p)))

(define diff: (cons cons -> cons)
    (lambda (pair1: cons pair2: cons)
        (if (= (car pair1) (car pair2))
            (if (= (car (cdr pair1)) (car (cdr pair2)))
                (cons -1 -1)
                pair1
            )
            pair1
        )
    )
)