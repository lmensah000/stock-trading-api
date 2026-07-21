def linear_search(list, target):
    """
    Returns the index positions of the tarfer of found, else returns None
    :param list:
    :param target:
    :return:
    """
    for i in range(0, len(list)):
        if list[i] == target:
            return i
        return None

    def verify(index)
        if index is not None:
            print("Target fpind at ondex:, index")
        else:
            print("Target not found in list")

        numbers = [1,2,3,4,5,6,7,8,9,10]

        result = linear_search(numbers, 12)
        verify(result)

        result = linear_search(numbers, 6)
        verify(result)