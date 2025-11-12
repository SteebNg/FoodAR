using UnityEngine;

public class InfoOnClicked : MonoBehaviour
{
    [SerializeField] private GameObject placeFoodUi;
    [SerializeField] private GameObject rotateFoodUi;

    void Start()
    {
        HideAllMenus();
        placeFoodUi.SetActive(true);
    }

    public void OpenPlaceFoodUi()
    {
        HideAllMenus();
        placeFoodUi.SetActive(true);
    }

    public void TransitionToRotateFoodUi()
    {
        placeFoodUi.SetActive(false);
        rotateFoodUi.SetActive(true);
    }

    public void CloseRotateFoodUi()
    {
        rotateFoodUi.SetActive(false);
    }

    private void HideAllMenus()
    {
        placeFoodUi.SetActive(false);
        rotateFoodUi.SetActive(false);
    }
}
