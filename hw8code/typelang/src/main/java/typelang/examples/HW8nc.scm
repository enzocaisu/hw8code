
(deref (ref: num 45))
(deref 45)


(set! (ref: num 0) 1)
(set! (ref: num 0) #t)


(car (cons 1 2))
(car 2)

(list : num 1 2 3 4 5)
(list : num 1 2 3 4 #f)

(define add : (num num num -> num) (lambda (x: num y: num z: num) (+x (+y z))))
(add 5 6 7)
(* 2 5)
(add 5 56 #t)
(3 4)

(< 3 4)
(> 3 #f)

(if (> 1 10) (list: num 1 1 2 3) (list: num 1 2 3 5))
(if (< 1 10) (list: num 1 2 3) 42)
(if 1 2 3)

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

(define padd : (cons num num -> num) (lambda (x: cons y: num z: num) (+ (car x) (+y z))))

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